# Entity, Boss & Spell System

Package: [`com.roguesmp.entity`](../src/main/java/com/roguesmp/entity)

> ⚠️ **Kiến trúc đã đổi hoàn toàn kể từ lần viết đầu của trang này.** Trước đây `BaseEntity` là 1 POJO Gson phẳng (field `baseStat`, `equipments`, `activeSpell`/`passiveSpell` trực tiếp) và boss được đăng ký qua 1 singleton `EntityRegistry.registerSpecial(id, factory)`. **Cả 2 điều đó không còn đúng.** `BaseEntity` giờ dùng [Codec System](Codec-System.md) và mô hình **component** y hệt [Item System](Item-System.md) — mọi dữ liệu/hành vi của 1 entity (tên hiển thị, cờ AI, stat, trang bị, spell, boss bar, nameplate, loot table, phase) đều là 1 `EntityComponent` cắm vào `Map<String, EntityComponent>`, không còn field cứng nào khác ngoài `id`/`entityType`.

## `BaseEntity` — định nghĩa tĩnh, dùng Codec + component

[`entity/BaseEntity.java`](../src/main/java/com/roguesmp/entity/BaseEntity.java):

```java
public static final Codec<BaseEntity> CODEC = Codec.composite(
        Codec.STRING.fieldOf("id").forGetter(BaseEntity::getId),
        Codec.enumOf(EntityType.class).fieldOf("entityType").forGetter(BaseEntity::getEntityType),
        Codec.<EntityComponent>dispatchedMap(Registries.ENTITY_COMPONENT_CODEC::getOrThrow)
                .optionalFieldOf("components", Map.of())
                .forGetter(BaseEntity::getComponents),
        BaseEntity::new
);
```

Chỉ 3 field: `id` (String, bắt buộc), `entityType` (enum `org.bukkit.entity.EntityType`, bắt buộc, khớp theo tên không phân biệt hoa/thường — không qua Gson nên `@SerializedName` trên `EntityType` không có tác dụng gì ở đây), `components` (`Map<String, EntityComponent>`, optional, mặc định rỗng — key của map chính là type-id, giống hệt `dispatchedMap` của `BaseItem`, xem [Codec System](Codec-System.md#polymorphic-dispatch-codecdispatch)). Registry: `Registries.ENTITY = new Registry<>("entities", BaseEntity.CODEC)`, thư mục `entities/`.

`processEntity(Entity)` gọi `component.apply(living)` cho mọi component — đây là bước gắn attribute/trang bị/cờ AI lên 1 `LivingEntity` Bukkit thật, chạy trước khi `SmpEntity` wrapper tồn tại.

## `EntityComponent` — điểm mở rộng, mirror `ItemComponent`

[`entity/component/EntityComponent.java`](../src/main/java/com/roguesmp/entity/component/EntityComponent.java), toàn bộ interface (mọi hook trừ `copy()` đều default no-op):

```java
public interface EntityComponent {
    @NotNull EntityComponent copy();                      // bắt buộc — tách state runtime khỏi template dùng chung
    default void apply(LivingEntity entity) { }             // 1 lần, lúc BaseEntity.processEntity — CHƯA có SmpEntity
    default void onSpawn(SmpEntity entity) { }               // 1 lần, lúc SmpEntity.initialize(), sau apply — ĐÃ có SmpEntity
    default void onUnload(SmpEntity entity) { }
    default void onDamage(DamageEvent event, SmpEntity entity) { }
    default void onHurt(DamageEvent event, SmpEntity entity) { }
    default void onDeath(EntityDeathEvent event, SmpEntity entity) { }
    default void onProjectileLaunch(ProjectileLaunchEvent event, SmpEntity entity) { }
    default void onProjectileHit(ProjectileHitEvent event, SmpEntity entity) { }
    default void onCastSpell(SpellCastEvent event, SmpEntity entity) { }
    default void onTargetEntity(EntityTargetLivingEntityEvent event, SmpEntity entity) { }
}
```

`SmpEntity` không có "mechanic layer" riêng nữa (khác với player, xem [Attribute System](Attribute-System.md#attribute-thực-sự-được-áp-dụng-như-thế-nào--không-có-attributemanager)) — mỗi hook Bukkit/tùy chỉnh nhận được chỉ đơn giản duyệt `componentMap.values()` và forward, y hệt `SmpEntity.onDamage`/`onHurt`/`onDeath`/... Component **là** hành vi, không có tầng trung gian nào khác.

Ticking định kỳ là 1 mixin riêng, [`TickingComponent`](../src/main/java/com/roguesmp/entity/component/TickingComponent.java):

```java
public interface TickingComponent extends EntityComponent {
    void tick(SmpEntity entity, int interval);
}
```

`SmpEntity` chỉ khởi động **1** task lịch chung (entity scheduler của Paper, Folia-safe) nếu có **ít nhất 1** component implement `TickingComponent` — không phải 1 task/component. Task này chạy mỗi `SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT` (2 tick) và gọi `tick(this, interval)` trên mọi component ticking. Component cần chu kỳ chậm hơn tự đếm ngược nội bộ và trừ `interval` mỗi lần gọi, không có tham số interval-riêng-per-component.

## `EntityComponentKeys` — đăng ký, mirror `ItemComponentKeys`

[`entity/component/EntityComponentKeys.java`](../src/main/java/com/roguesmp/entity/component/EntityComponentKeys.java):

```java
public static final EntityComponentKey<DisplayNameComponent> DISPLAY_NAME;
static {
    DISPLAY_NAME = register("display_name", DisplayNameComponent.CODEC);
    BEHAVIOR = register("behavior", BehaviorComponent.CODEC);
    ATTRIBUTES = register("attributes", AttributeComponent.CODEC);
    EQUIPMENT = register("equipment", EquipmentComponent.CODEC);
    SPELLS = register("spells", SpellComponent.CODEC);
    BOSS_BAR = register("boss_bar", BossBarComponent.CODEC);
    NAMEPLATE = register("nameplate", NameplateComponent.CODEC);
    PHASE = new EntityComponentKey<>("phase"); // code-driven only, không có CODEC (BossHealthAction là lambda, không serialize được)
    LOOT_TABLE = register("loot_table", LootTableComponent.CODEC);
}
public static void loadClass() { } // gọi từ RogueSmpCore.init() để ép static initializer chạy — cùng pattern ItemComponentKeys
```

## Viết entity bằng JSON

Bảng đầy đủ mọi component (`display_name`, `behavior`, `attributes`, `equipment`, `spells`, `boss_bar`, `nameplate`, `loot_table`) — từng key, kiểu, mặc định, tác dụng — cùng danh sách spell JSON và ví dụ entity hoàn chỉnh nằm ở [JSON-Entities](JSON-Entities.md). Trang này chỉ giải thích cơ chế bên trong.

## `SpellComponent` — casting chủ động & bị động

[`entity/component/impl/SpellComponent.java`](../src/main/java/com/roguesmp/entity/component/impl/SpellComponent.java) gộp cả phần **JSON-khai báo** lẫn phần **runtime sống** (lịch chạy Folia-safe, detection range, dispatch event) vào cùng 1 component:

```java
public static final Codec<SpellComponent> CODEC = Codec.composite(
        Codec.listOf(SpellConfig.CODEC).optionalFieldOf("spells", List.of()).forGetter(SpellComponent::getSpellConfigs),
        Codec.BOOLEAN.optionalFieldOf("canCastSameSpellTwice", false).forGetter(SpellComponent::isCanCastSameSpellTwice),
        SpellComponent::new
);
```

Mỗi phần tử của `spells` là 1 `SpellConfig(SpellType, SpellParams)`, dispatch trên field `"type"` qua [`Registries.ENTITY_SPELL`](../src/main/java/com/roguesmp/entity/spell/EntitySpells.java). Active hay passive không khai trong JSON: spell nào là `ActiveSpell` thì vào vòng cast, còn lại chỉ nhận `tick(...)` + các hook sự kiện.

```json
"spells": {
  "spells": [ { "type": "summon_spell", "entityIds": ["goblin_warrior"] }, { "type": "slow_aura_spell" } ],
  "canCastSameSpellTwice": false
}
```

Ví dụ `SelfDestructSpell.Params`:

```java
public record Params(int particleCount) implements SpellParams {
    public static final Codec<Params> CODEC = Codec.INT.optionalFieldOf("particleCount", 10)
            .xmap(Params::new, Params::particleCount).codec();
}
```

Lịch chạy (`tick` mỗi `SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT` = 2 tick): mọi spell nhận `tick(context, interval)`; vòng active là máy trạng thái `IDLE → WINDUP → RECOVERY`. Ở `IDLE`, sau `castLockUntil`, chọn ngẫu nhiên theo `weight()` trong các `ActiveSpell` đã hết hồi chiêu và `canCast(...)` (loại spell vừa cast trừ khi `canCastSameSpellTwice` hoặc chỉ có 1 spell). `startCast` đặt hồi chiêu, bắn `SpellCastEvent`, gọi `onWindupStart`; `onWindupTick` mỗi chu kỳ tới khi đủ `windup()` → `onCast` → `RECOVERY` trong `recovery()` tick. Mất mục tiêu khi niệm → `onInterrupted`. Không còn player trong `detectionRange` → ngủ (hủy cast, ngừng tick); hook sự kiện vẫn chạy. Boss có thể đổi bộ spell bằng `setSpells(...)` hoặc ép cast bằng `forceCast(...)`.

### Boss code-driven không khai spell trong JSON

Boss viết tay không cần `spells` trong JSON — tự dựng spell rồi gắn bằng constructor `new SpellComponent(List<Spell> spells, int detectionRange, int startDelay, boolean canCastSameSpellTwice)` (ví dụ thật: [`VeinStalker`](../src/main/java/com/roguesmp/entity/boss/veinstalker/VeinStalker.java)):

```java
public class MyBoss extends SmpEntity {
    public MyBoss(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        setComponent(EntityComponentKeys.SPELLS, new SpellComponent(
                List.of(new WebSnareSpell(entity), new DarknessSpell(entity)),
                24, 40, false));
    }
}
```

Chuyển pha: `spellComponent.setSpells(List<Spell> next, int castDelay)` — hủy cast đang niệm, gọi `onRemoved()` cho spell bị bỏ (spell giữ lại cùng instance thì giữ nguyên trạng thái/hồi chiêu), chặn cast mới trong `castDelay` tick.

<a id="phasecomponent--ngưỡng-máu-chuyển-pha"></a>
## `PhaseComponent` — ngưỡng máu chuyển pha

[`entity/component/impl/PhaseComponent.java`](../src/main/java/com/roguesmp/entity/component/impl/PhaseComponent.java) — 1 `TickingComponent` độc lập, tách hẳn khỏi `SpellComponent` (trước đây là 1 `PhaseManager` lồng bên trong spell casting; giờ phase trigger không cần spell nào tồn tại để hoạt động). Nhận 1 `Map<Integer, BossHealthAction>` (% máu → hành động, `BossHealthAction` là `void run(LivingEntity boss)` — 1 lambda Java, **không thể khai trong JSON**) cộng cờ `capDamage` (chặn 1 đòn quá to "nhảy vọt" qua 1 ngưỡng mà không kích hoạt nó):

```java
Map<Integer, PhaseComponent.BossHealthAction> phaseEvents = new HashMap<>();
phaseEvents.put(70, boss -> spellComponent.setSpells(phase2Spells, 20));
phaseEvents.put(30, boss -> { /* triệu hồi minion, đổi pha cuối */ });

setComponent(EntityComponentKeys.PHASE, new PhaseComponent(phaseEvents, true));
```

Ngưỡng được kiểm tra theo thứ tự giảm dần, "tiêu thụ" từng cái khi máu đi qua; nếu `capDamage == true`, HP thật của entity bị ép khớp đúng ngưỡng đó (không đi qua `DamageEvent`, nên nếu có `NameplateComponent` gắn kèm, `PhaseComponent` tự refresh nameplate luôn).

## ⚠️ Trạng thái hiện tại của boss/entity đặc biệt — đang dở dang

[`registry/entity` cũ / `EntityFactory`](../src/main/java/com/roguesmp/entity/EntityFactory.java) là cơ chế **thiết kế để thay thế** `EntityRegistry.registerSpecial(id, factory)` cũ:

```java
@FunctionalInterface
public interface EntityFactory<T extends SmpEntity> {
   T create(BaseEntity base, LivingEntity living);
}
```

`EntityManager.wrap(base, living)` tra `Registries.ENTITY_FACTORY.get(base.getId())`, fallback về `SmpEntity` thường nếu không tìm thấy — đúng khuôn mẫu cũ. **Nhưng tại thời điểm viết trang này, [`SpecialEntities.java`](../src/main/java/com/roguesmp/entity/SpecialEntities.java) — nơi lẽ ra khai báo mọi `EntityFactory` — có toàn bộ khai báo bị comment lại, và `entity/boss/` chỉ còn đúng 1 file `KeasaTheLich.java`, hiện là 1 class rỗng chưa implement gì.** Nghĩa là **không có id entity nào hiện được coi là "đặc biệt"** — mọi entity, kể cả tương lai sẽ là boss, hiện wrap thành `SmpEntity` thường. Các boss cũ (`HellKnight`, `PrimordialSlime`, `HellKnightCompanion`, ...) được nhắc trong doc comment của `SpellComponent`/`EntityRegistry` cũ **không còn tồn tại như file code** — đừng tìm chúng, và đừng lấy các tên đó làm ví dụ thật khi viết code mới.

**Khi hồi sinh 1 boss** (hoặc viết boss đầu tiên trong kiến trúc mới): viết class boss `extends SmpEntity`, gắn `SpellComponent`/`PhaseComponent` của riêng nó (xem 2 mục trên), rồi đăng ký bằng cách bỏ comment (hoặc thêm dòng tương tự) trong `SpecialEntities.java`:

```java
public static final EntityFactory<MyBoss> MY_BOSS = register("my_boss_id", MyBoss::new);
```

Id truyền vào `register` phải khớp chính xác `id` trong file JSON của entity đó — lệch thì boss âm thầm rơi về `SmpEntity` thường, không báo lỗi gì.

## Cách thêm 1 loại spell mới (do JSON điều khiển)

1. Tạo `record Params(...) implements SpellParams` với `public static final Codec<Params> CODEC` (mọi field nên `optionalFieldOf` — `register` decode `{}` để lấy mặc định, thiếu là crash lúc khởi động).
2. Viết class spell: extend `Spell` (passive — override `tick`/hook sự kiện) hoặc `ActiveSpell` (gọi `super(cooldown, windup, recovery)`, cài `onCast`, tùy chọn `canCast`/`onWindupStart`/`onWindupTick`/`onInterrupted`).
3. Đăng ký cả type-id lẫn params-codec vào [`EntitySpells.java`](../src/main/java/com/roguesmp/entity/spell/EntitySpells.java) (tương tự `ItemComponentKeys`/`ComponentKeys` — 1 `SpellType<Params>` gói cả factory dựng `Spell` thật lẫn `CODEC` của `Params`).
4. Dùng `"type": "your_id"` trong mảng `spells` của bất kỳ entity JSON nào.

## Lưu ý & lỗi thường gặp

- **`BaseEntity` giờ dùng Codec + component, không còn là Gson POJO phẳng** — copy pattern từ `BaseItem`/`ItemComponent` khi thêm component mới, không tự chế 1 field trực tiếp trên `BaseEntity`.
- **`EntityComponentKeys.loadClass()` phải được gọi trong `RogueSmpCore.init()`** (cùng lúc với `ItemComponentKeys.loadClass()`) — bỏ sót thì codec của mọi entity component không được đăng ký, mọi file `entities/*.json` fail decode.
- **`phase` không thể khai trong JSON** — `BossHealthAction` là 1 lambda Java thuần, chỉ gắn được từ code (`setComponent(EntityComponentKeys.PHASE, ...)`), khác với mọi component khác trong bảng.
- **Đăng ký sai/thiếu id trong `SpecialEntities.java`** khiến 1 boss âm thầm chạy như `SmpEntity` thường — không có exception nào báo, chỉ đơn giản là không có phase/spell riêng nào chạy.
- **`TickingComponent.tick(entity, interval)` không tick nhanh hơn `SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT` (2 tick)** — 1 component cần chu kỳ chậm hơn (chậm hơn 2 tick) nên tự đếm ngược nội bộ và trừ `interval` mỗi lần `tick` được gọi.

---
◀ [GUI Framework](GUI-Framework.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Player Ability System](Player-Ability-System.md)
