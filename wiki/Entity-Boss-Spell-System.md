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

<a id="tham-khảo-đầy-đủ--mọi-component-entity-đã-đăng-ký"></a>
## Tham khảo đầy đủ — mọi component entity đã đăng ký

| Key JSON | Class | Shape |
| :--- | :--- | :--- |
| `display_name` | `DisplayNameComponent` | chuỗi MiniMessage trần |
| `behavior` | `BehaviorComponent` | `{ "noAi": false, "invulnerable": false, "persistent": false, "detectionRange": 20 }` (tất cả optional) |
| `attributes` | `AttributeComponent` | map phẳng `EntityAttribute -> double`, vd. `{"max_health": 200.0, "movement_speed": 0.3}` |
| `equipment` | `EquipmentComponent` | map phẳng `EquipmentSlot (vanilla) -> EntityEquipment` |
| `spells` | `SpellComponent` | xem [mục Spell](#spellcomponent--casting-chủ-động--bị-động) bên dưới |
| `boss_bar` | `BossBarComponent` | `{ "range": 30, "color": "WHITE", "style": "PROGRESS", "bossFog": true }` (tất cả optional) |
| `nameplate` | `NameplateComponent` | `{ "showHealth": true, "showName": true, "heightOffset": 0.3 }` (tất cả optional — mọi entity tự có 1 bản mặc định nếu không khai) |
| `loot_table` | `LootTableComponent` | mảng chuỗi trần — id trỏ vào [`Registries.LOOT_TABLE`](Loot-System.md) |
| `phase` | `PhaseComponent` | **không JSON-hóa được** — chỉ gắn từ code, xem [mục Phase](#phasecomponent--ngưỡng-máu-chuyển-pha) bên dưới |

`EntityAttribute` (enum, `entity/EntityAttribute.java`, wrap `org.bukkit.attribute.Attribute`): `MAX_HEALTH, FOLLOW_RANGE, KNOCKBACK_RESISTANCE, MOVEMENT_SPEED, FLYING_SPEED, ATTACK_DAMAGE, ATTACK_KNOCKBACK, ATTACK_SPEED, ARMOR, FALL_DAMAGE_MULTIPLIER, SAFE_FALL_DISTANCE, SCALE, STEP_HEIGHT, GRAVITY, JUMP_STRENGTH, BURNING_TIME, EXPLOSION_KNOCKBACK_RESISTANCE, MOVEMENT_EFFICIENCY, WATER_MOVEMENT_EFFICIENCY`.

`EntityEquipment` (`entity/EntityEquipment.java`) fields: `material` (bắt buộc), `displayName`/`lore` (optional), `enchantGlint` (optional, mặc định `false`), `trimMaterial`/`trimPattern` (optional, key vanilla trim, vd. `"redstone"`/`"silence"`), `dyeColor` (optional, `"A,R,G,B"`), `headSkin` (optional, id trong `SkinRegistry`).

### Ví dụ đầy đủ — `entities/hell_knight.json`

```json
{
  "id": "hell_knight",
  "entityType": "ZOMBIE",
  "components": {
    "display_name": "<red>Hell Knight",
    "behavior": { "invulnerable": false, "persistent": true, "detectionRange": 25 },
    "attributes": { "max_health": 200.0, "movement_speed": 0.3, "attack_damage": 15.0 },
    "equipment": {
      "hand": { "material": "NETHERITE_SWORD", "displayName": "<red>Hellfire Blade", "enchantGlint": true },
      "head": { "material": "NETHERITE_HELMET", "trimMaterial": "redstone", "trimPattern": "silence" }
    },
    "spells": {
      "activeSpell": [ { "type": "self_destruct_spell", "particleCount": 20 } ],
      "passiveSpell": [ { "type": "slow_aura_spell" } ],
      "passiveInterval": 40
    },
    "boss_bar": { "range": 40, "color": "RED", "style": "NOTCHED_10" },
    "nameplate": { "heightOffset": 0.35 },
    "loot_table": ["hell_knight_common", "hell_knight_rare"]
  }
}
```

## `SpellComponent` — casting chủ động & bị động

[`entity/component/impl/SpellComponent.java`](../src/main/java/com/roguesmp/entity/component/impl/SpellComponent.java) gộp cả phần **JSON-khai báo** lẫn phần **runtime sống** (lịch chạy Folia-safe, detection range, dispatch event) vào cùng 1 component:

```java
public static final Codec<SpellComponent> CODEC = Codec.composite(
        Codec.listOf(SpellParams.CODEC).optionalFieldOf("activeSpell", List.of()).forGetter(SpellComponent::getActiveSpellParams),
        Codec.listOf(SpellParams.CODEC).optionalFieldOf("passiveSpell", List.of()).forGetter(SpellComponent::getPassiveSpellParams),
        Codec.INT.optionalFieldOf("passiveInterval", 0).forGetter(SpellComponent::getPassiveIntervalConfig),
        Codec.BOOLEAN.optionalFieldOf("canCastSameSpellTwice", false).forGetter(SpellComponent::isCanCastSameSpellTwiceConfig),
        SpellComponent::new
);
```

Mỗi phần tử của `activeSpell`/`passiveSpell` là 1 `SpellParams` đa hình, dispatch trên field `"type"` qua [`Registries.ENTITY_SPELL`](../src/main/java/com/roguesmp/entity/spell/EntitySpells.java) (mỗi `SpellType` đăng ký cả id lẫn `Codec` params riêng của nó). `passiveInterval` ≤ 0 → dùng mặc định `SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT` (2 tick).

```json
"spells": {
  "activeSpell": [ { "type": "self_destruct_spell", "particleCount": 20 } ],
  "passiveSpell": [ { "type": "slow_aura_spell" } ],
  "passiveInterval": 40,
  "canCastSameSpellTwice": false
}
```

Ví dụ `SelfDestructSpell.Params`, cho thấy shape 1 `SpellParams` cụ thể:

```java
public record Params(int particleCount) implements SpellParams {
    public static final Codec<Params> CODEC = Codec.INT.optionalFieldOf("particleCount", 10)
            .xmap(Params::new, Params::particleCount).codec();
    @Override public String getTypeId() { return "self_destruct_spell"; }
}
```

`lucSpell` lịch chạy: `activeSpell` chọn 1 spell qua `SpellManager` (`floor((số-spell-1)/2)` cooldown chống chọn lại ngay), timer tiếp theo lấy từ `cooldownTicks()` của spell vừa cast; `passiveSpell` tick **mọi** spell mỗi chu kỳ, tự tắt (`activeSpells.cancelAll()`) khi không còn player nào trong `detectionRange`.

### Boss code-driven không khai spell trong JSON

Boss viết tay không cần `spells` trong JSON — dùng constructor `new SpellComponent(SmpEntity owner)`, bind ngay lập tức để có thể gọi `startSpell(...)`/`changePhase(...)` từ chính constructor của boss:

```java
public class MyBoss extends SmpEntity {
    private final SpellComponent spellComponent;

    public MyBoss(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        this.spellComponent = new SpellComponent(this);
        setComponent(EntityComponentKeys.SPELLS, spellComponent);
    }

    @Override
    protected void onInitialized() {
        spellComponent.startSpell(new SpellManager(phase1Actives), phase1Passives, detectionRange);
    }
}
```

`changePhase(SpellManager newActive, List<Spell> newPassive, @Nullable Consumer<LivingEntity> phaseAction)` (+ overload có `spellDelay`) cancel spell hiện tại rồi thay bằng list mới — primitive cốt lõi để chuyển pha kiểu boss.

<a id="phasecomponent--ngưỡng-máu-chuyển-pha"></a>
## `PhaseComponent` — ngưỡng máu chuyển pha

[`entity/component/impl/PhaseComponent.java`](../src/main/java/com/roguesmp/entity/component/impl/PhaseComponent.java) — 1 `TickingComponent` độc lập, tách hẳn khỏi `SpellComponent` (trước đây là 1 `PhaseManager` lồng bên trong spell casting; giờ phase trigger không cần spell nào tồn tại để hoạt động). Nhận 1 `Map<Integer, BossHealthAction>` (% máu → hành động, `BossHealthAction` là `void run(LivingEntity boss)` — 1 lambda Java, **không thể khai trong JSON**) cộng cờ `capDamage` (chặn 1 đòn quá to "nhảy vọt" qua 1 ngưỡng mà không kích hoạt nó):

```java
Map<Integer, PhaseComponent.BossHealthAction> phaseEvents = new HashMap<>();
phaseEvents.put(70, boss -> this.changePhase(new SpellManager(phase2Active), phase2Passive, null));
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

1. Tạo `record`/class implement `SpellParams` (`getTypeId()` trả type-id, cộng `public static final Codec<...> CODEC`).
2. Viết class `Spell` thật (extend `entity/spell/Spell.java` — `run(int interval)`, `cooldownTicks()` là 2 method abstract) đọc dữ liệu từ `Params`.
3. Đăng ký cả type-id lẫn params-codec vào [`EntitySpells.java`](../src/main/java/com/roguesmp/entity/spell/EntitySpells.java) (tương tự `ItemComponentKeys`/`ComponentKeys` — 1 `SpellType<Params>` gói cả factory dựng `Spell` thật lẫn `CODEC` của `Params`).
4. Dùng `"type": "your_id"` trong `activeSpell`/`passiveSpell` của bất kỳ entity JSON nào.

## Lưu ý & lỗi thường gặp

- **`BaseEntity` giờ dùng Codec + component, không còn là Gson POJO phẳng** — copy pattern từ `BaseItem`/`ItemComponent` khi thêm component mới, không tự chế 1 field trực tiếp trên `BaseEntity`.
- **`EntityComponentKeys.loadClass()` phải được gọi trong `RogueSmpCore.init()`** (cùng lúc với `ItemComponentKeys.loadClass()`) — bỏ sót thì codec của mọi entity component không được đăng ký, mọi file `entities/*.json` fail decode.
- **`phase` không thể khai trong JSON** — `BossHealthAction` là 1 lambda Java thuần, chỉ gắn được từ code (`setComponent(EntityComponentKeys.PHASE, ...)`), khác với mọi component khác trong bảng.
- **Đăng ký sai/thiếu id trong `SpecialEntities.java`** khiến 1 boss âm thầm chạy như `SmpEntity` thường — không có exception nào báo, chỉ đơn giản là không có phase/spell riêng nào chạy.
- **`TickingComponent.tick(entity, interval)` không tick nhanh hơn `SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT` (2 tick)** dù bạn có gọi `startSpell`/cấu hình gì khác — 1 component cần chu kỳ chậm hơn (chậm hơn 2 tick) nên tự đếm ngược nội bộ và trừ `interval` mỗi lần `tick` được gọi.

---
◀ [GUI Framework](GUI-Framework.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Player Ability System](Player-Ability-System.md)
