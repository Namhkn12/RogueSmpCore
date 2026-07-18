# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

RogueSmpCore is a custom-content plugin for a Minecraft **Paper 1.21.11** server ("Rework plugin", package `com.roguesmp`). Java 21, built with Gradle. It layers a custom item/attribute/ability/dungeon framework on top of vanilla Bukkit, and depends on several other plugins being present at runtime (`depend` in `plugin.yml`): CommandAPI, FastAsyncWorldEdit, TAB, PlaceholderAPI, Multiverse-Core, WorldGuard.

There is no unit test suite in this repo (no JUnit dependency, no `src/test`) — verification happens by running/attaching to a live Paper server.

## Build & run commands

```sh
./gradlew build          # compile + assemble
./gradlew shadowJar       # build the relocated fat jar (com.roguesmp.libs.glowingentities)
./gradlew runServer       # boot a local Paper 1.21 test server with this plugin loaded (via run-paper plugin)
```

`runServer` is the primary way to manually verify changes — it downloads/caches a Paper server and drops the built jar in automatically. Note the runtime `depend` plugins (CommandAPI, FAWE, TAB, PlaceholderAPI, Multiverse-Core, WorldGuard) are `compileOnly` and are **not** auto-provisioned into the `runServer` sandbox, so features that hard-depend on them may not fully exercise in that environment unless those plugins are dropped into the test server's `plugins/` folder manually.

There is no configured lint/format task; match the surrounding code's style.

## High-level architecture

Everything bootstraps from `RogueSmpCore` (`src/main/java/com/roguesmp/RogueSmpCore.java`), the `JavaPlugin` entrypoint. `onEnable()` runs, in order: `init()` (construct every manager/registry singleton), `loadData()` (read persisted JSON state), `initListeners()`, `initCommands()`. `onDisable()` kicks players and calls `saveData()`. Read this file first when tracing how any subsystem wires together — it's the single place all top-level singletons are constructed and ordered.

### Registry / Manager singleton convention

Almost every subsystem (`ItemRegistry`, `BlockRegistry`, `EntityRegistry`, `AbilityRegistry`, `NpcRegistry`, `QuestRegistry`, `SkinRegistry`, `PlayerManager`, `IslandManager`, `EffectManager`, `BlockManager`, ...) follows the same hand-rolled pattern: private constructor, static `INSTANCE`, `static void init(...)` called once from `RogueSmpCore.init()`, `static X getInstance()`. **There is no shared base class/interface** for this — it's a repeated convention, not an abstraction, so don't go looking for a common `Registry<T>` type.

Data-backed registries load `*.json` from `plugin.getDataFolder()/<folder>` via a shared Gson instance (`com.roguesmp.utils.Utils.GSON`) in a `loadFromFile()`/`loadData()`/`loadAll()` method called from `RogueSmpCore.loadData()`. Some (`AbilityRegistry`, `EntityRegistry`) are hybrids: code hard-registers special/boss entries in the constructor (behavior), then JSON overlays tunable data (numbers, descriptions) at load time. Polymorphic JSON (quest objectives/rewards/requirements, npc actions, upgrade requirements) is dispatched by a `"type"` string field to per-kind sub-registries exposing `create(type, jsonNode)`.

### Item system (`item/`)

Two-tier model:
- **`BaseItem`** — immutable JSON-defined template: `Material base` + `Map<String, ItemComponent> components`. Owned by `ItemRegistry`.
- **`SmpItem`** — the live wrapper around a real `ItemStack`; get one via `SmpItem.wrap(...)` (backed by `ItemManager`'s cache), never construct ad hoc.

`ItemComponent` (`item/component/ItemComponent.java`) is the extension point: `copy()` plus optional hooks `contributeLore(ItemLoreContext)`, `modifyStack(ItemDataContext)`, `load(PersistentDataContainerView)`, `save(PersistentDataContainer)`. `SmpItem.generateItemStack(...)` is the assembly pipeline — builds the stack, applies `ItemModifier`s, then for every component: `save` → `modifyStack` → `contributeLore`, finally writing accumulated lore.

Components are addressed by `ComponentKey<T>` constants declared centrally in `constant/ComponentKeys.java`; that class's static initializer also registers each component's `ComponentCodec` into `ItemComponentCodecRegistry` (`registry/`) so the polymorphic `Map<String, ItemComponent>` on `BaseItem` can round-trip through Gson via `ComponentMapCodec`. `ComponentKeys.loadClass()` exists solely to force that static initializer to run at startup — call it, don't remove it.

When adding a new item component: implement `ItemComponent`, add a `ComponentKey` + codec registration in `ComponentKeys`, and decide whether it needs PDC persistence (`load`/`save`, see `DurabilityComponent`) or is lore/stack-only (see `BrokenComponent`, `NameComponent`). Fields that shouldn't round-trip through the JSON definition (e.g. runtime-only or PDC-only state) are annotated `@GsonIgnore`.

PersistentDataContainer keys used across the plugin (item id, item uuid, applied gem, mob id, npc id) live in `constant/Keys.java` under the shared `"smp"` namespace — don't invent ad hoc `NamespacedKey`s elsewhere.

### Attribute system (`attribute/`)

`SmpAttribute` is an interface with mostly-default no-op hooks covering vanilla-style attribute wrapping (`addVanillaAttribute`/`removeVanillaAttribute`, for stacking an `AttributeModifier` onto a real Bukkit `Attribute`) *and* a broad custom gameplay-event surface (`onDamageEntity`, `onKillEntity`, `onProjectileHit`, `onCombust`, etc.) for attributes with no vanilla equivalent (e.g. `MagicDamagePercent` hooks a custom `DamageEvent`/`DamageType` pipeline). There's no `AttributeRegistry` — each attribute is registered by being instantiated directly as an `Attributes` enum constant (`constant/Attributes.java`), which is also what Gson serializes item attribute maps against.

### GUI framework (`gui/`)

All GUIs extend `BaseGui` (`gui/BaseGui.java`, `implements InventoryHolder`), which wraps a real `Inventory` and a `Map<Integer, ClickHandler>`. Implement the abstract `setup()` to lay out slots via `addButton(slot, itemStack, clickHandler)` / `addAction` / `addItem` / `fillEmpty()`. `gui/interfaces/` only holds two optional mixins (`IHaveBlueprint`, `IHaveInputOutput`) for recipe/machine-style GUIs. Click routing is centralized in `listener/GuiListener.java`, which checks `event.getView().getTopInventory().getHolder(false) instanceof BaseGui` and dispatches to `BaseGui`'s overridable `onClickTopInventory`/`onClickBottomInventory`/`onDragInventory`/`onOpenInventory`/`onCloseInventory` — don't add new per-GUI `InventoryClickEvent` listeners, hook into these instead.

### Dungeon system (`dungeon/`)

The largest and most layered subsystem, bootstrapped separately via `DungeonRegistry.onEnable(plugin, itemRegistry)` / `onDisable()` at the end of `RogueSmpCore.init()`/`saveData()`. Strict bottom-up layering, all wired by hand (constructor injection, no DI framework):

1. **`data/definition/`** — immutable, JSON-loadable templates (`Dungeon`, room/loot/objective/spawner definitions). Doc comments explicitly warn: runtime progress must not live here.
2. **`data/runtime/`** (+ `runtime/session/`) — live mutable session state per active run: `DungeonInstance` (root aggregate) composed of `DungeonSession`, `DungeonProgress`, `DungeonPlayer`, `DungeonTimer`, plus `Party`, `RegionInstance`, `RoomInstance`, `SpawnerInstance`.
3. **`repository/` + `repository/impl/`** — one `I*Repository` per aggregate, thin JSON persistence (load/save/delete) against the plugin data folder.
4. **`manager/`** — in-memory caches over repositories (e.g. `InstanceManager` holds `Map<String, DungeonInstance>`, loads all on construction, exposes `saveAll()`).
5. **`service/` + `service/impl/`** — business logic, one `I*Service` interface per concern, composed of multiple managers/services (e.g. `RoomNavigationService` coordinates `IPartyService`, `InstanceManager`, `RoomManager`, `ISchematicService`, ... to move a party between rooms). Some services (`DungeonLifecycleService`, `RoomRuntimeService`, `TreasureService`, etc.) are concrete-only, no interface.
6. **`controller/`** — feature-level façades composing services (`DungeonFlowController`, `BossRoomController`, `PartyController`, `SchemetaController`).
7. **`actor/`** — the only layer that touches Bukkit input directly: `actor/command/` (CommandAPI commands), `actor/listener/` (Bukkit `Listener`s that call into controllers), `actor/ui/` (dungeon-specific `BaseGui`s), `actor/scoreboard/` (TAB integration).

When changing dungeon behavior, find the right layer first — game rules belong in `service`/`controller`, not `actor`; persistence shape belongs in `data/runtime` + `repository`, not scattered through services.

### Entity / boss / spell system (`entity/`)

Mirrors the item system's definition/runtime split: `BaseEntity` is the static JSON definition (base stats, equipment, spell id lists); `SmpEntity` is the runtime wrapper around a live `LivingEntity`, driving periodic active/passive spell ticks via Folia-safe `entity.getScheduler().runAtFixedRate`. Bosses subclass `SmpEntity` directly (`entity/boss/hellknight/HellKnight`, `entity/boss/primordialslime/PrimordialSlime`), defining per-phase active/passive spell lists and swapping them on boss-bar HP thresholds via `changePhase(...)`. Boss-specific spells live alongside their boss (`entity/boss/hellknight/*Spell.java`), not in a shared `spell/impl/`. Special entity classes are wired into `EntityRegistry` via `registerSpecial(id, BiFunction<BaseEntity, LivingEntity, SmpEntity>)`; everything else falls back to plain `SmpEntity::new`.

### Player ability system (`player/ability/`)

`Ability` is the per-cast behavior class (abstract, same default-hook pattern as `SmpAttribute`/`Spell`), paired with a static `AbilityInfo<T>` (declared as `Ability.INFO` on each impl) holding the factory, per-level `scaling` values, named `AbilityAction`s, and a `triggerMap` (populated from JSON at load time) binding `AbilityTrigger`s (physical key + option predicates, see `player/ability/trigger/`) to action names. `AbilityLoadout` is the per-player runtime container (`Map<AbilityType, Ability[]>`) with an interceptor pattern (`contextOwner`) for abilities that capture subsequent input (aim/charge modes); `cast(key)` → `execute(...)` fires a cancellable `AbilityCastEvent`, then interprets the returned `AbilityResponse`'s `InputSignal` (`CONTINUE`/`CONSUME`/`CAPTURE`/`RELEASE`/`DENY`) to decide whether to keep processing the ability chain.

## Conventions to follow

- New cross-cutting extension points (item components, attributes, abilities, spells) are consistently modeled as an interface/abstract class with mostly-default no-op hooks over a shared gameplay-event surface (damage, kill, consume, projectile, combust, etc.) — follow that shape rather than inventing a new mechanism.
- PDC keys go through `constant/Keys.java`; item component identity/codecs go through `constant/ComponentKeys.java`; don't hand-roll `NamespacedKey`s or Gson type adapters elsewhere.
- GUIs go through `BaseGui` + `GuiListener`; don't register new raw `InventoryClickEvent` listeners.
- Registries/managers are constructed and ordered explicitly in `RogueSmpCore.init()`/`loadData()`/`initListeners()`/`initCommands()` — when adding a new subsystem, wire it in there in the right phase (dependencies must be constructed before dependents that `getInstance()` them).
