# Codec System

Package: [`com.roguesmp.codec`](../src/main/java/com/roguesmp/codec)

> Được thiết kế theo **DataFixerUpper (DFU)** của Mojang. Tính chất quan trọng nhất là **độc lập định dạng dữ liệu** (format-agnostic): cùng 1 `Codec<A>` có thể chuyển đổi qua lại giữa JSON, NBT, YAML,... mà không cần đụng vào logic encode/decode — chỉ cần đổi implementation của `DynamicOps<O>`.
>
> Hiện tại codebase chỉ có duy nhất 1 implementation của `DynamicOps` là [`JsonOps`](../src/main/java/com/roguesmp/codec/JsonOps.java) (dựa trên `JsonElement` của Gson), được [`Registry#loadFrom`](../src/main/java/com/roguesmp/registry/Registry.java) dùng để load mọi file `*.json` nằm trong `plugin.getDataFolder()`. Xem [Registry System](Registry-System.md) để biết codec được gắn vào quá trình load như thế nào.

## Khái niệm cốt lõi

| Kiểu | File | Vai trò |
| :--- | :--- | :--- |
| `Codec<A>` | `Codec.java` | Bộ chuyển đổi 2 chiều giữa 1 Java object `A` và dữ liệu đã serialize `O`. Có 2 method cốt lõi: `encode(A, DynamicOps<O>)`, `decode(O, DynamicOps<O>)`. |
| `MapCodec<A>` | `MapCodec.java` | Một `Codec` chuyên biệt, đọc/ghi trực tiếp các cặp key-value **bên trong** 1 map đã có sẵn, thay vì tạo ra 1 value đứng riêng. Vì nhắm vào field, nhiều `MapCodec` có thể được gộp phẳng vào 1 object cha thông qua `composite(...)`. |
| `DynamicOps<O>` | `DynamicOps.java` | Trừu tượng hóa định dạng dữ liệu (JSON, NBT, ...). Toàn bộ logic Codec chỉ gọi qua `ops` (`createInt`, `getString`, `setMapEntry`, ...), nên logic decode/encode không bao giờ phụ thuộc vào 1 định dạng cụ thể. |
| `DataResult<T>` | `DataResult.java` | Record chứa `result` hoặc `error`. Không bao giờ throw khi dữ liệu sai định dạng — lỗi được truyền đi như 1 giá trị kèm message. |
| `JsonOps` | `JsonOps.java` | Implementation `DynamicOps<JsonElement>` duy nhất hiện có. Truy cập qua singleton `JsonOps.INSTANCE`. |

## Các Codec cơ bản (Primitives)

```java
Codec.STRING    // String
Codec.BOOLEAN   // Boolean
Codec.BYTE      // Byte
Codec.SHORT     // Short
Codec.INT       // Integer
Codec.LONG      // Long
Codec.FLOAT     // Float
Codec.DOUBLE    // Double
Codec.UUID      // java.util.UUID
Codec.MATERIAL  // org.bukkit.Material, khớp theo tên
Codec.KEY       // net.kyori.adventure.key.Key (vd. "minecraft:stone")

Codec<GameMode> GAMEMODE_CODEC = Codec.enumOf(GameMode.class);
```

Mọi codec phức hợp khác trong dự án đều được xây từ những viên gạch primitive này thông qua các phép biến đổi bên dưới.

## Biến đổi kiểu dữ liệu (Transformations)

### `xmap(to, from)` — cả 2 chiều đều luôn thành công

```java
// Đây chính là định nghĩa thật của Codec.KEY:
Codec<Key> KEY = Codec.STRING.xmap(Key::key, Key::asString);

// Từ DurabilityComponent — lưu maxDurability như 1 số nguyên đứng riêng, không phải field:
public static final Codec<DurabilityComponent> CODEC =
        Codec.INT.xmap(DurabilityComponent::new, DurabilityComponent::maxDurability);
```

### `comapFlatMap(to, from)` — decode có thể lỗi, encode thì không

```java
// Đây chính là định nghĩa thật của Codec.MATERIAL:
Codec<Material> MATERIAL = Codec.STRING.comapFlatMap(
        name -> {
            Material mat = Material.matchMaterial(name);
            return mat != null ? DataResult.success(mat) : DataResult.error("Unknown material: " + name);
        },
        Material::name
);
```

### `flatXmap(to, from)` — cả 2 chiều đều có thể lỗi

Dùng khi cả encode lẫn decode đều cần validate/chuyển đổi với khả năng thất bại.

## Field cho Object (nhóm `fieldOf`)

Biến 1 `Codec<A>` đứng riêng thành 1 `MapCodec<A>` gắn với 1 key.

| Method | Thiếu field | Dữ liệu sai định dạng |
| :--- | :--- | :--- |
| `fieldOf("key")` | báo lỗi | báo lỗi |
| `optionalFieldOf("key")` | `Optional.empty()` | báo lỗi |
| `optionalFieldOf("key", defaultVal)` | `defaultVal` | báo lỗi |
| `optionalFieldOf("key", () -> defaultVal)` | kết quả của supplier | báo lỗi |
| `lenientOptionalFieldOf("key", fallback)` | `fallback` | **bỏ qua lỗi**, trả về `fallback` |

```java
MapCodec<String> NAME = Codec.STRING.fieldOf("name");
MapCodec<Integer> AGE = Codec.INT.optionalFieldOf("age", 18);
MapCodec<Double> HEALTH = Codec.DOUBLE.lenientOptionalFieldOf("health", 20.0);
```

> ⚠️ `optionalFieldOf("key", defaultVal)` **bỏ qua việc ghi field khi encode** nếu giá trị hiện tại bằng đúng `defaultVal` (so sánh bằng `Objects.equals`) — giúp JSON gọn hơn. Nghĩa là `A` nên có `equals()` hợp lý (record thì tự có sẵn). Ví dụ thật, từ [`SmpEffect`](../src/main/java/com/roguesmp/effect/SmpEffect.java):
> ```java
> Codec.enumOf(DeathBehavior.class)
>         .optionalFieldOf("death_behavior", DeathBehavior.REMOVE_ON_DEATH)
>         .forGetter(BaseProperties::deathBehavior)
> ```

## Gộp nhiều field thành Object (`composite`)

`Codec.composite(...)` (hoặc `MapCodec.composite(...)`) gộp 1–12 field thành 1 class/record. Mỗi field là 1 `MapCodec` được gắn với `.forGetter(...)` để đọc lại giá trị đó từ object cha.

```java
public record PlayerData(String name, int level, double health) {
    public static final Codec<PlayerData> CODEC = Codec.composite(
            Codec.STRING.fieldOf("name").forGetter(PlayerData::name),
            Codec.INT.optionalFieldOf("level", 1).forGetter(PlayerData::level),
            Codec.DOUBLE.lenientOptionalFieldOf("health", 20.0).forGetter(PlayerData::health),
            PlayerData::new // factory — thứ tự tham số phải khớp thứ tự field ở trên
    );
}
```

### Ví dụ thật — record lồng nhau: `PotionContentComponent`

Từ [`PotionContentComponent`](../src/main/java/com/roguesmp/item/component/impl/PotionContentComponent.java): 1 `composite` kết hợp với `listOf(...)` của 1 composite codec *khác*.

```java
public static final Codec<PotionContentComponent> CODEC = Codec.composite(
        Codec.STRING.fieldOf("color").forGetter(PotionContentComponent::getColor),
        Codec.listOf(StoredEffect.CODEC).fieldOf("effects").forGetter(PotionContentComponent::getEffects),
        PotionContentComponent::new
);

public record StoredEffect(String effectType, int duration, int amplifier) {
    public static final Codec<StoredEffect> CODEC = Codec.composite(
            Codec.STRING.fieldOf("type").forGetter(StoredEffect::effectType),
            Codec.INT.fieldOf("duration").forGetter(StoredEffect::duration),
            Codec.INT.fieldOf("amplifier").forGetter(StoredEffect::amplifier),
            StoredEffect::new
    );
}
```

```json
{
  "color": "255,255,0,0",
  "effects": [
    { "type": "speed", "duration": 200, "amplifier": 1 },
    { "type": "regeneration", "duration": 100, "amplifier": 0 }
  ]
}
```

## Tập hợp dữ liệu (Collections)

### `listOf(elementCodec)`

```java
Codec<List<String>> STRING_LIST = Codec.listOf(Codec.STRING);
```

### `unboundedMap(keyCodec, valueCodec)` — key động, 1 codec value cố định

Key bắt buộc phải encode ra được String. Ví dụ thật — 2 tầng `unboundedMap` lồng nhau trong [`GemDataComponent`](../src/main/java/com/roguesmp/item/component/impl/GemDataComponent.java) (`Map<EquipSlot, Map<Attributes, Double>>`):

```java
public static final Codec<GemDataComponent> CODEC = Codec.composite(
        Codec.unboundedMap(
                Codec.enumOf(EquipSlot.class),
                Codec.unboundedMap(Codec.enumOf(Attributes.class), Codec.DOUBLE)
        ).fieldOf("attributes").forGetter(GemDataComponent::getAttributes),
        Codec.DOUBLE.optionalFieldOf("success_chance", 1d).forGetter(GemDataComponent::getSuccessChance),
        GemDataComponent::new
);
```

Ví dụ 1 tầng đơn giản hơn từ [`EquipAttributeComponent`](../src/main/java/com/roguesmp/item/component/impl/EquipAttributeComponent.java):

```java
public static final Codec<EquipAttributeComponent> CODEC = Codec.composite(
        Codec.unboundedMap(Codec.enumOf(Attributes.class), Codec.DOUBLE).fieldOf("attributes")
                .forGetter(EquipAttributeComponent::getBaseAttributes),
        Codec.enumOf(EquipSlot.class).fieldOf("slot").forGetter(EquipAttributeComponent::getSlot),
        EquipAttributeComponent::new
);
```

### `dispatchedMap(keyCodec, codecGetter)` — key động, **key quyết định codec của value**

Dùng cho `Map<String, ItemComponent>` của `BaseItem` — xem bên dưới.

<a id="polymorphic-dispatch-codecdispatch"></a>
## Xử lý đa hình (`Codec.dispatch`)

Chọn Codec của subclass dựa trên 1 field định danh kiểu (mặc định tên `"type"`, có thể tùy chỉnh). Đây là cách `SmpEffect` xử lý `SpeedEffect`, `DamageIncreaseEffect`, `ResistanceEffect`, ... — mỗi subclass tự có `Codec` riêng, còn lớp cha chỉ cần biết cách tra ra đúng codec.

### Lớp cha — [`SmpEffect`](../src/main/java/com/roguesmp/effect/SmpEffect.java)

```java
public static final Codec<SmpEffect> CODEC = Codec.dispatch(
        "id",                                 // tên field lưu type-id trong JSON (tùy chỉnh, mặc định là "type")
        SmpEffect::getEffectID,                // lấy type-id từ 1 instance
        Codec.STRING,                          // codec của chính type-id
        Registries.EFFECT_CODEC::getOrThrow    // tra registry để lấy Codec con tương ứng
);
```

### Đăng ký codec con — [`EffectCodecRegistry`](../src/main/java/com/roguesmp/registry/EffectCodecRegistry.java)

```java
public static void bootstrap() {
    register(SpeedEffect.EFFECT_ID, SpeedEffect.CODEC);
    register(DamageIncreaseEffect.ID, DamageIncreaseEffect.CODEC);
    register(ResistanceEffect.ID, ResistanceEffect.CODEC);
}
```

`bootstrap()` chạy 1 lần từ `Registries.boostrap(plugin)`, đổ dữ liệu vào `Registries.EFFECT_CODEC` (1 `Registry<Codec<? extends SmpEffect>>` in-memory).

### Giả lập kế thừa bằng 1 `MapCodec` field chung

`SmpEffect` cung cấp sẵn 1 `MapCodec<BaseProperties>` cho các field dùng chung (`duration`, `death_behavior`, `display`, `display_time`):

```java
public record BaseProperties(int duration, DeathBehavior deathBehavior, boolean display, boolean displayTime) {}

public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
        Codec.INT.fieldOf("duration").forGetter(BaseProperties::duration),
        Codec.enumOf(DeathBehavior.class)
                .optionalFieldOf("death_behavior", DeathBehavior.REMOVE_ON_DEATH)
                .forGetter(BaseProperties::deathBehavior),
        Codec.BOOLEAN.optionalFieldOf("display", true).forGetter(BaseProperties::display),
        Codec.BOOLEAN.optionalFieldOf("display_time", true).forGetter(BaseProperties::displayTime),
        BaseProperties::new
);
```

Lớp con, [`SpeedEffect`](../src/main/java/com/roguesmp/effect/impl/SpeedEffect.java), chỉ cần "gắn" `BASE_CODEC` như 1 field bình thường vào `composite(...)` của chính nó:

```java
public static final Codec<SpeedEffect> CODEC = Codec.composite(
        SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
        Codec.DOUBLE.fieldOf("value").forGetter(SpeedEffect::getMagnitude),
        Codec.STRING.fieldOf("modifierId").forGetter(SpeedEffect::getModifierId),
        SpeedEffect::new // SpeedEffect(BaseProperties, double, String)
);
```

JSON kết quả (field của `BASE_CODEC` và của `SpeedEffect` nằm phẳng chung 1 object, `"id"` được `dispatch` tự chèn vào):

```json
{
  "id": "speed",
  "duration": 200,
  "death_behavior": "keep_on_death",
  "value": 0.2,
  "modifierId": "boots_of_speed"
}
```

> **Muốn thêm 1 loại effect mới**: tạo class extends `SmpEffect`, khai `CODEC` bằng `Codec.composite(SmpEffect.BASE_CODEC.forGetter(...), <field riêng>..., YourClass::new)`, rồi đăng ký nó trong `EffectCodecRegistry.bootstrap()`. Không đụng vào `SmpEffect.CODEC`.

<a id="full-worked-example--baseitem-dispatch--dispatchedmap--registry-loading"></a>
## Ví dụ phức tạp đầy đủ — `BaseItem` (dispatch + dispatchedMap + load qua registry)

[`BaseItem`](../src/main/java/com/roguesmp/item/BaseItem.java) kết hợp gần như mọi kỹ thuật ở trên vào 1 codec thật — 1 item có thể mang bất kỳ tổ hợp component nào, mỗi cái 1 kiểu con khác nhau, được xác định bằng tên nằm trong map `"components"`:

```java
public static final Codec<BaseItem> CODEC = Codec.composite(
        Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
        Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
        Codec.BOOLEAN.optionalFieldOf("unique", false).forGetter(BaseItem::isUnique),
        Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                .optionalFieldOf("components", Map.of())
                .forGetter(BaseItem::getComponents),
        BaseItem::new
);
```

- Mỗi component đăng ký `Codec` của nó vào `Registries.ITEM_COMPONENT_CODEC` qua [`ItemComponentCodecRegistry.register(id, codec)`](../src/main/java/com/roguesmp/registry/ItemComponentCodecRegistry.java), được gọi từ static initializer của [`ComponentKeys`](../src/main/java/com/roguesmp/constant/ComponentKeys.java) (vd. `ITEM_NAME = ItemComponentCodecRegistry.register("name", NameComponent.CODEC);`). Xem [Item System](Item-System.md) để có bức tranh đầy đủ.
- Khác với `dispatch()` (đọc type-id từ 1 field cố định *bên trong* value, vd. `"id"`), `dispatchedMap` dùng **chính key của map** (`"name"`, `"durability"`, ...) làm type-id.

```json
{
  "id": "fire_sword",
  "base": "NETHERITE_SWORD",
  "unique": true,
  "components": {
    "name": "<gold>Fire Sword",
    "durability": 500,
    "attribute": { "attributes": { "ATTACK_DAMAGE": 12.0 }, "slot": "MAINHAND" }
  }
}
```

### Pipeline load

`BaseItem.CODEC` là backend của `Registries.ITEM = new Registry<>("items", BaseItem.CODEC)`. Trong `RogueSmpCore.loadData()` → `Registries.loadAllData(plugin)` → [`Registry#loadFrom`](../src/main/java/com/roguesmp/registry/Registry.java):

```java
JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
DataResult<T> result = codec.decode(json, JsonOps.INSTANCE);
if (result.isSuccess()) {
    register(id, result.result());
} else {
    RogueSmpCore.LOGGER.error("Failed to decode [{}] in '{}': {}", id, locationKey, result.error());
}
```

Một file JSON bị lỗi chỉ log lỗi cho đúng entry đó — không bao giờ làm sập cả quá trình load. Xem [Registry System](Registry-System.md) để biết toàn bộ câu chuyện load registry.

## Quick start tối giản

```java
public record SkillConfig(String id, int level, List<Material> requiredItems) {
    public static final Codec<SkillConfig> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(SkillConfig::id),
            Codec.INT.optionalFieldOf("level", 1).forGetter(SkillConfig::level),
            Codec.listOf(Codec.MATERIAL).optionalFieldOf("items", List.of()).forGetter(SkillConfig::requiredItems),
            SkillConfig::new
    );
}

SkillConfig skill = new SkillConfig("fireball", 5, List.of(Material.BLAZE_POWDER, Material.FIRE_CHARGE));
DataResult<JsonElement> encoded = SkillConfig.CODEC.encode(skill, JsonOps.INSTANCE);

DataResult<SkillConfig> decoded = SkillConfig.CODEC.decode(encoded.result(), JsonOps.INSTANCE);
if (decoded.isSuccess()) {
    System.out.println("Loaded skill: " + decoded.result().id());
} else {
    System.err.println("Decode failed: " + decoded.error());
}
```

## Lưu ý & lỗi thường gặp

- **`dispatchedMap` / `unboundedMap` yêu cầu key encode ra được String.** Nếu `keyCodec.encode(...)` không tạo ra 1 JSON string, sẽ nhận lỗi `"Map key did not encode as a string"`.
- **Thứ tự field trong `composite(...)` phải khớp thứ tự tham số của factory** — factory chỉ nhận các giá trị đã decode theo đúng vị trí.
- **`fieldOf` báo lỗi khi thiếu field; các biến thể `optionalFieldOf`/`lenientOptionalFieldOf` thì không.** Dùng `fieldOf` cho dữ liệu thực sự bắt buộc (id, base material); dùng biến thể optional khi có 1 giá trị mặc định hợp lý.
- **`lenientOptionalFieldOf` nuốt luôn cả lỗi decode**, không chỉ lỗi thiếu field — hữu ích để khoan dung dữ liệu cũ/hỏng, nhưng dễ bị lạm dụng để che giấu bug thật.
- **Codec của subtype phải được đăng ký trước khi codec cha decode.** Với `dispatch`/`dispatchedMap`, registry tra cứu (`Registries.EFFECT_CODEC`, `Registries.ITEM_COMPONENT_CODEC`) phải được populate (qua `bootstrap()`/static initializer) trước khi bất kỳ JSON nào được load — kiểm tra thứ tự trong `RogueSmpCore.init()`.
- **Không tự viết `TypeAdapter` của Gson nếu đã có sẵn Codec cho kiểu đó.** Hãy decode qua `JsonOps.INSTANCE` thay vào đó — giữ mọi thứ độc lập định dạng và tái sử dụng được các combinator có sẵn.

---
◀ Về [Trang chủ](Home.md) · Tiếp theo: [Registry System](Registry-System.md)
