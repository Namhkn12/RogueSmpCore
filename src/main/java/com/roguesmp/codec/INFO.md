# Codec System API Documentation

> Bộ API `Codec` này được thiết kế dựa trên thư viện **DataFixerUpper (DFU)** của Mojang. Điểm mạnh nhất của kiến trúc này là **độc lập với định dạng dữ liệu** (Format-Agnostic): cùng một `Codec`, ta có thể chuyển đổi qua lại giữa JSON, NBT, YAML,... mà không cần sửa đổi logic xử lý — chỉ cần đổi implementation của `DynamicOps`.
>
> Hiện tại repo mới chỉ có một implementation duy nhất của `DynamicOps` là [`JsonOps`](JsonOps.java) (dựa trên Gson `JsonElement`), được dùng bởi [`Registry#loadFrom`](../registry/Registry.java) để load mọi `*.json` trong `plugin.getDataFolder()`.

## 1. Các Khái Niệm Cốt Lõi

| Thành phần | File | Mô tả |
| :--- | :--- | :--- |
| `Codec<A>` | [Codec.java](Codec.java) | Đối tượng chuyển đổi 2 chiều giữa Java Object kiểu `A` và dữ liệu đã serialized (`O`). Có 2 method cốt lõi: `encode(A, DynamicOps<O>)` và `decode(O, DynamicOps<O>)`. |
| `MapCodec<A>` | [MapCodec.java](MapCodec.java) | Phiên bản đặc biệt của `Codec`, thao tác trực tiếp lên các cặp Key-Value **bên trong** một Map/Object đang có sẵn (thay vì tạo ra 1 value đứng riêng). Nhờ vậy nhiều `MapCodec` có thể được "gộp phẳng" vào chung 1 object bằng `composite(...)`. |
| `DynamicOps<O>` | [DynamicOps.java](DynamicOps.java) | Layer trừu tượng hóa định dạng dữ liệu (JSON, NBT, v.v.). Toàn bộ Codec chỉ thao tác qua `ops` (`createInt`, `getString`, `setMapEntry`, ...) nên logic decode/encode độc lập hoàn toàn với định dạng cụ thể. |
| `DataResult<T>` | [DataResult.java](DataResult.java) | Record wrapper chứa `result` (nếu thành công) hoặc `error` (nếu thất bại) — không bao giờ throw exception khi decode sai định dạng, giúp xử lý lỗi an toàn và trả message rõ ràng. |
| `JsonOps` | [JsonOps.java](JsonOps.java) | `DynamicOps<JsonElement>` — implementation duy nhất hiện có, dùng Gson. Truy cập qua singleton `JsonOps.INSTANCE`. |

---

## 2. Các Codec Cơ Bản (Primitives)

Có sẵn trong `Codec` dưới dạng hằng số static hoặc factory method:

```java
Codec.STRING    // String
Codec.BOOLEAN   // Boolean
Codec.BYTE      // Byte
Codec.SHORT     // Short
Codec.INT       // Integer
Codec.LONG      // Long
Codec.FLOAT     // Float
Codec.DOUBLE    // Double
Codec.UUID      // java.util.UUID (dạng chuỗi UUID chuẩn)
Codec.MATERIAL  // org.bukkit.Material (khớp theo tên qua Material.matchMaterial)
Codec.KEY       // net.kyori.adventure.key.Key (vd. "minecraft:stone")

// Tạo Codec cho Enum bất kỳ (so khớp không phân biệt hoa/thường, tự uppercase):
Codec<GameMode> GAMEMODE_CODEC = Codec.enumOf(GameMode.class);
```

Tất cả các Codec phức hợp trong plugin đều được xây từ những viên gạch primitive này.

---

## 3. Biến Đổi Kiểu Dữ Liệu (Transformations)

Khi đã có một `Codec<A>`, ta có thể tạo ra `Codec<B>` bằng các hàm ánh xạ (mapping) — đây là cách các Codec "phức tạp hơn" (`Codec.MATERIAL`, `Codec.KEY`, `Codec.UUID`, `Codec.enumOf`) thực chất được xây dựng trong [Codec.java](Codec.java).

### `xmap(to, from)` — chuyển đổi **luôn thành công**

Dùng khi chuyển đổi giữa 2 kiểu dữ liệu không có rủi ro quăng lỗi.

```java
// Codec.KEY thực chất được định nghĩa đúng như thế này:
Codec<Key> KEY = Codec.STRING.xmap(Key::key, Key::asString);
```

Một ví dụ khác lấy từ [`DurabilityComponent`](../item/component/impl/DurabilityComponent.java) — lưu độ bền tối đa dưới dạng 1 số nguyên đứng riêng (không phải field trong object):

```java
public static final Codec<DurabilityComponent> CODEC =
        Codec.INT.xmap(DurabilityComponent::new, DurabilityComponent::maxDurability);
```

### `comapFlatMap(to, from)` — chiều **decode có thể thất bại**

Dùng khi đọc vào (decode) có thể lỗi, còn ghi ra (encode) luôn an toàn. Đây chính là cách `Codec.MATERIAL`, `Codec.UUID` và `Codec.enumOf` được implement:

```java
// Codec.MATERIAL thực chất trong Codec.java:
Codec<Material> MATERIAL = Codec.STRING.comapFlatMap(
        name -> {
            Material mat = Material.matchMaterial(name);
            return mat != null ? DataResult.success(mat) : DataResult.error("Unknown material: " + name);
        },
        Material::name
);
```

### `flatXmap(to, from)` — **cả 2 chiều đều có thể thất bại**

Dùng khi cả decode lẫn encode đều có khả năng lỗi (ví dụ: chuyển đổi giữa 2 kiểu dữ liệu có ràng buộc validation ở cả 2 chiều).

---

## 4. Định Nghĩa Field Cho Object (`fieldOf`)

Chuyển đổi một standalone `Codec<A>` thành `MapCodec<A>` gắn với 1 Key cụ thể bên trong Object.

| Method | Hành vi khi thiếu Field | Hành vi khi dữ liệu lỗi |
| :--- | :--- | :--- |
| `fieldOf("key")` | Báo lỗi (`DataResult.error`) | Báo lỗi |
| `optionalFieldOf("key")` | Trả `Optional.empty()` | Báo lỗi |
| `optionalFieldOf("key", defaultVal)` | Trả `defaultVal` | Báo lỗi |
| `optionalFieldOf("key", () -> defaultVal)` | Trả kết quả supplier | Báo lỗi |
| `lenientOptionalFieldOf("key", fallback)` | Trả `fallback` | **Bỏ qua lỗi**, trả `fallback` |

```java
MapCodec<String> NAME = Codec.STRING.fieldOf("name");
MapCodec<Integer> AGE = Codec.INT.optionalFieldOf("age", 18);
MapCodec<Double> HEALTH = Codec.DOUBLE.lenientOptionalFieldOf("health", 20.0);
```

> ⚠️ Lưu ý về `optionalFieldOf("key", defaultVal)`: khi **encode**, nếu giá trị hiện tại bằng đúng `defaultVal` (so bằng `Objects.equals`), field đó **sẽ không được ghi ra JSON** — giúp file JSON gọn hơn. Điều này có nghĩa nếu kiểu `A` là 1 object/record, nó nên có `equals()` hợp lý (record tự có sẵn).

Ví dụ thật từ [`SmpEffect`](../effect/SmpEffect.java):

```java
Codec.enumOf(DeathBehavior.class)
        .optionalFieldOf("death_behavior", DeathBehavior.REMOVE_ON_DEATH)
        .forGetter(BaseProperties::deathBehavior)
```

---

## 5. Ghép Nhiều Field Tạo Object (`composite`)

Dùng `Codec.composite(...)` (hoặc `MapCodec.composite(...)`) để kết hợp nhiều field riêng lẻ thành một Class/Record hoàn chỉnh. Hỗ trợ từ **1 đến 12 field**.

### Ví dụ đơn giản

```java
public record PlayerData(String name, int level, double health) {
    public static final Codec<PlayerData> CODEC = Codec.composite(
            Codec.STRING.fieldOf("name").forGetter(PlayerData::name),
            Codec.INT.optionalFieldOf("level", 1).forGetter(PlayerData::level),
            Codec.DOUBLE.lenientOptionalFieldOf("health", 20.0).forGetter(PlayerData::health),
            PlayerData::new // Factory constructor khớp thứ tự tham số với các field ở trên
    );
}
```

`.forGetter(...)` gắn 1 getter vào `MapCodec<A>` để tạo ra `RecordField<A, R>` — đây là thứ `composite` cần để biết cách **đọc field đó ra từ object cha** khi encode.

### Ví dụ thật: `PotionContentComponent` (composite + nested record)

Từ [`PotionContentComponent`](../item/component/impl/PotionContentComponent.java) — minh họa 1 record lồng bên trong 1 component khác, và `listOf` đi kèm 1 composite codec khác:

```java
public class PotionContentComponent implements ItemComponent {

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
    // ...
}
```

JSON tương ứng:

```json
{
  "color": "255,255,0,0",
  "effects": [
    { "type": "speed", "duration": 200, "amplifier": 1 },
    { "type": "regeneration", "duration": 100, "amplifier": 0 }
  ]
}
```

---

## 6. Tập Hợp Dữ Liệu (Combinators)

### Danh sách: `listOf(elementCodec)`

```java
Codec<List<String>> STRING_LIST = Codec.listOf(Codec.STRING);
```

### Map Cố Định Kiểu Giá Trị: `unboundedMap(keyCodec, valueCodec)`

Dùng khi Map có key không cố định trước nhưng **mọi value đều cùng 1 Codec** (vd. `Map<Attributes, Double>`). **Key luôn phải encode ra được String.**

Ví dụ thật, lồng 2 tầng `unboundedMap` trong [`GemDataComponent`](../item/component/impl/GemDataComponent.java) (`Map<EquipSlot, Map<Attributes, Double>>`):

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

Và ví dụ đơn giản hơn (chỉ 1 tầng) trong [`EquipAttributeComponent`](../item/component/impl/EquipAttributeComponent.java):

```java
public static final Codec<EquipAttributeComponent> CODEC = Codec.composite(
        Codec.unboundedMap(Codec.enumOf(Attributes.class), Codec.DOUBLE).fieldOf("attributes")
                .forGetter(EquipAttributeComponent::getBaseAttributes),
        Codec.enumOf(EquipSlot.class).fieldOf("slot").forGetter(EquipAttributeComponent::getSlot),
        EquipAttributeComponent::new
);
```

### Map Đa Kiểu: `dispatchedMap(keyCodec, codecGetter)`

Dùng khi **chính key** quyết định Codec nào sẽ được dùng để encode/decode value tương ứng — tức mỗi entry trong map có thể là 1 kiểu con khác nhau. Đây là cơ chế đứng sau `Map<String, ItemComponent>` của [`BaseItem`](../item/BaseItem.java) (xem mục 8).

```java
static <V> Codec<Map<String, V>> dispatchedMap(Function<String, Codec<? extends V>> codecGetter)
```

---

## 7. Xử Lý Đa Hình (Polymorphic Dispatch — `Codec.dispatch`)

Tự động phân loại Subclass dựa trên 1 field định danh (mặc định tên `"type"`, có thể tùy chỉnh). Đây là cơ chế dùng cho `SmpEffect` — mỗi loại effect (`SpeedEffect`, `DamageIncreaseEffect`, `ResistanceEffect`, ...) tự khai báo `Codec` riêng, còn `SmpEffect.CODEC` chỉ cần biết cách tra registry để tìm đúng codec con.

### 7.1. Khai báo trong lớp cha — [`SmpEffect`](../effect/SmpEffect.java)

```java
public abstract class SmpEffect implements Comparable<SmpEffect>, DisplayableEffect, Cloneable {

    public static final Codec<SmpEffect> CODEC = Codec.dispatch(
            "id",                           // Tên field lưu type-key trong JSON (ở đây dùng "id" thay vì mặc định "type")
            SmpEffect::getEffectID,         // Lấy type-key từ 1 instance đã có
            Codec.STRING,                   // Codec của type-key
            Registries.EFFECT_CODEC::getOrThrow // Tra registry để lấy Codec con tương ứng
    );
    // ...
}
```

### 7.2. Đăng ký Codec con — [`EffectCodecRegistry`](../registry/EffectCodecRegistry.java)

```java
public class EffectCodecRegistry {
    public static void bootstrap() {
        register(SpeedEffect.EFFECT_ID, SpeedEffect.CODEC);
        register(DamageIncreaseEffect.ID, DamageIncreaseEffect.CODEC);
        register(ResistanceEffect.ID, ResistanceEffect.CODEC);
    }
}
```

`bootstrap()` được gọi 1 lần từ [`Registries.boostrap(plugin)`](../registry/Registries.java), lưu các `Codec<? extends SmpEffect>` vào `Registries.EFFECT_CODEC` (1 `Registry<Codec<? extends SmpEffect>>` in-memory).

### 7.3. Kết hợp `BASE_CODEC` (giả lập kế thừa)

Vì Java không có cách "thừa kế field" tự nhiên trong Codec, `SmpEffect` khai báo sẵn 1 `MapCodec<BaseProperties>` chứa các field chung (`duration`, `death_behavior`, `display`, `display_time`):

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

Lớp con — [`SpeedEffect`](../effect/impl/SpeedEffect.java) — chỉ cần "chèn" `SmpEffect.BASE_CODEC` như 1 field bình thường vào `composite(...)` của chính nó, cạnh các field riêng của nó:

```java
public class SpeedEffect extends SmpEffect {
    public static final String EFFECT_ID = "speed";

    public static final Codec<SpeedEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(SpeedEffect::getMagnitude),
            Codec.STRING.fieldOf("modifierId").forGetter(SpeedEffect::getModifierId),
            SpeedEffect::new // constructor SpeedEffect(BaseProperties, double, String)
    );
    // ...
}
```

Kết quả JSON của 1 `SpeedEffect` (các field của `BASE_CODEC` và của `SpeedEffect` nằm phẳng chung 1 object, cộng thêm `"id"` do `dispatch` tự chèn vào khi encode):

```json
{
  "id": "speed",
  "duration": 200,
  "death_behavior": "keep_on_death",
  "value": 0.2,
  "modifierId": "boots_of_speed"
}
```

> Muốn thêm 1 loại effect mới: tạo class extends `SmpEffect`, khai `CODEC` bằng `Codec.composite(SmpEffect.BASE_CODEC.forGetter(...), <field riêng>..., Constructor::new)`, rồi đăng ký nó trong `EffectCodecRegistry.bootstrap()`. Không cần đụng vào `SmpEffect.CODEC`.

---

## 8. Ví Dụ Phức Tạp Đầy Đủ: `BaseItem` (dispatch + dispatchedMap + registry loading)

[`BaseItem`](../item/BaseItem.java) là ví dụ tổng hợp gần như mọi kỹ thuật ở trên trong 1 Codec thực tế — 1 item có thể chứa **bất kỳ tổ hợp component nào**, mỗi component là 1 kiểu con khác nhau, quyết định bởi tên key trong map `"components"`:

```java
public class BaseItem {
    public static final Codec<BaseItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
            Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
            Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                    .optionalFieldOf("components", Map.of())
                    .forGetter(BaseItem::getComponents),
            BaseItem::new
    );
    // ...
}
```

- Mỗi component (`NameComponent`, `DurabilityComponent`, `EquipAttributeComponent`, ...) đăng ký `Codec` của nó vào `Registries.ITEM_COMPONENT_CODEC` thông qua [`ItemComponentCodecRegistry.register(id, codec)`](../registry/ItemComponentCodecRegistry.java), được gọi từ static initializer của [`ComponentKeys`](../constant/ComponentKeys.java) (vd. `ITEM_NAME = ItemComponentCodecRegistry.register("name", NameComponent.CODEC);`).
- Khi decode `"components"`, `dispatchedMap` dùng chính **tên key** (`"name"`, `"durability"`, ...) làm type-key để tra ra đúng `Codec` cho từng entry — khác với `dispatch()` ở mục 7 vốn đọc type-key từ 1 field cố định (`"id"`/`"type"`) **bên trong** value.
- `BaseItem` không còn field `"unique"` trong JSON — `isUnique()` được tính tự động: true nếu bất kỳ component nào trong `"components"` implement marker interface [`UniqueTrackingComponent`](../item/component/UniqueTrackingComponent.java) (vd. `DurabilityComponent`, `EnchantComponent`, `GemSocketComponent` — những component có state per-instance lưu qua PDC). Không cần khai báo unique thủ công nữa.

JSON ví dụ của 1 `BaseItem`:

```json
{
  "id": "fire_sword",
  "base": "NETHERITE_SWORD",
  "components": {
    "name": "<gold>Fire Sword",
    "durability": 500,
    "attribute": {
      "attributes": { "ATTACK_DAMAGE": 12.0 },
      "slot": "MAINHAND"
    }
  }
}
```

### Toàn bộ pipeline load từ file

`BaseItem.CODEC` được gắn vào `Registries.ITEM = new Registry<>("items", BaseItem.CODEC)` ([`Registries.java`](../registry/Registries.java)). Khi `RogueSmpCore.loadData()` chạy → `Registries.loadAllData(plugin)` → [`Registry#loadFrom`](../registry/Registry.java):

```java
JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
DataResult<T> result = codec.decode(json, JsonOps.INSTANCE);
if (result.isSuccess()) {
    register(id, result.result());
} else {
    RogueSmpCore.LOGGER.error("Failed to decode [{}] in '{}': {}", id, locationKey, result.error());
}
```

Đây chính là lý do `DataResult` tồn tại: file JSON lỗi sẽ chỉ log lỗi rõ ràng (kèm message do chính Codec sinh ra) và bỏ qua entry đó, **không làm crash toàn bộ quá trình load**.

---

## 9. Quick Start Tối Giản

```java
public class CodecExample {
    public record SkillConfig(String id, int level, List<Material> requiredItems) {
        public static final Codec<SkillConfig> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(SkillConfig::id),
                Codec.INT.optionalFieldOf("level", 1).forGetter(SkillConfig::level),
                Codec.listOf(Codec.MATERIAL).optionalFieldOf("items", List.of()).forGetter(SkillConfig::requiredItems),
                SkillConfig::new
        );
    }

    public static void main(String[] args) {
        // 1. Encode Java Object -> JsonElement
        SkillConfig skill = new SkillConfig("fireball", 5, List.of(Material.BLAZE_POWDER, Material.FIRE_CHARGE));
        DataResult<JsonElement> encoded = SkillConfig.CODEC.encode(skill, JsonOps.INSTANCE);
        JsonElement json = encoded.result();

        // 2. Decode JsonElement -> Java Object
        DataResult<SkillConfig> decoded = SkillConfig.CODEC.decode(json, JsonOps.INSTANCE);
        if (decoded.isSuccess()) {
            SkillConfig result = decoded.result();
            System.out.println("Loaded skill: " + result.id() + " Lvl " + result.level());
        } else {
            System.err.println("Decode failed: " + decoded.error());
        }
    }
}
```

---

## 10. Lưu Ý & Lỗi Thường Gặp

- **`dispatchedMap` / `unboundedMap` yêu cầu key encode ra String.** Nếu `keyCodec.encode(...)` không trả về 1 JSON string (vd. dùng nhầm 1 Codec object phức tạp làm key), sẽ nhận lỗi `"Map key did not encode as a string"`.
- **Thứ tự tham số trong `composite(...)` phải khớp đúng thứ tự tham số constructor/factory method** truyền vào cuối cùng — factory chỉ nhận lần lượt kết quả decode của từng field theo đúng thứ tự khai báo.
- **`fieldOf` báo lỗi khi thiếu field, `optionalFieldOf`/`lenientOptionalFieldOf` thì không.** Chọn `fieldOf` cho dữ liệu bắt buộc phải có (id, base material, ...), dùng các biến thể optional cho dữ liệu có thể suy ra mặc định hợp lý.
- **`lenientOptionalFieldOf` nuốt luôn lỗi decode**, không chỉ lỗi thiếu field — hữu ích khi migrate dữ liệu cũ/broken nhưng dễ che giấu bug nếu lạm dụng, chỉ nên dùng khi thực sự cần khoan dung.
- **Đăng ký Codec con trước khi Codec cha decode.** Với `dispatch`/`dispatchedMap`, registry tra codec (`Registries.EFFECT_CODEC`, `Registries.ITEM_COMPONENT_CODEC`) phải được populate (qua `bootstrap()`/static initializer) trước khi bất kỳ file JSON nào được load — xem thứ tự gọi trong `RogueSmpCore.init()`.
- **Không tự viết Gson `TypeAdapter` cho kiểu dữ liệu mới nếu đã có Codec.** Định nghĩa 1 `Codec<T>` rồi decode qua `JsonOps.INSTANCE` thay vì thêm logic parse JSON thủ công — giữ mọi thứ format-agnostic và tái dùng được các combinator sẵn có.
