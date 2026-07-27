# Registry System

Package: [`com.roguesmp.registry`](../src/main/java/com/roguesmp/registry)

> ⚠️ **Đang trong quá trình migration.** Codebase đang dần rời khỏi kiểu mỗi subsystem tự viết 1 singleton riêng (`ItemRegistry`, `BlockRegistry`, `EntityRegistry`, ...) để chuyển sang 1 class trung tâm duy nhất là [`Registries`](../src/main/java/com/roguesmp/registry/Registries.java), giữ các instance `Registry<T>` đã gõ kiểu (typed) dưới dạng static field. Code mới nên dùng thẳng `Registries.<TÊN>` — xem [Quá trình migration](#the-migration-registry-getinstance--registries) bên dưới.

## Hai thứ cùng tên "Registry" — đừng nhầm lẫn

| | Là gì | Ở đâu |
| :--- | :--- | :--- |
| `Registry<T>` | 1 **class** dùng chung, tổng quát. 1 instance = 1 tập hợp entry đã gõ kiểu, có thể được backing bởi JSON hoặc không. | [`Registry.java`](../src/main/java/com/roguesmp/registry/Registry.java) |
| `Registries` | 1 **class chứa** các `public static final Registry<T>` — nơi duy nhất mọi instance `Registry<T>` trong plugin được khai báo. | [`Registries.java`](../src/main/java/com/roguesmp/registry/Registries.java) |

Ngoài ra, hầu hết các subsystem khác (`PlayerManager`, `EffectManager`, `BlockManager`, và các class cũ như `ItemRegistry`/`BlockRegistry`/`EntityRegistry`/`NpcRegistry`/`QuestRegistry`/`SkinRegistry`) theo 1 convention singleton tự viết tay **không liên quan** — xem [Convention singleton Manager](#convention-singleton-manager-khong-lien-quan-nhung-o-khap-noi) bên dưới. Không có base type dùng chung cho convention này; đây chỉ là 1 khuôn mẫu lặp lại, không phải abstraction.

## `Registry<T>`

```java
public class Registry<T> {
    private final Map<String, T> entries = new HashMap<>();
    private final String locationKey;   // tên thư mục con dưới data folder, vd. "items"
    private final Codec<T> codec;       // xem Codec System

    // Data-driven: entry được load từ file *.json
    public Registry(String locationKey, Codec<T> codec) { ... }

    // Chỉ in-memory: không load gì từ đĩa, entry được đăng ký bằng tay
    public Registry() { this.locationKey = null; this.codec = null; }
    ...
}
```

Mọi `Registry(String, Codec)` được khởi tạo ở bất kỳ đâu trong plugin sẽ tự đăng ký vào 1 list static tên `DATA_REGISTRIES`. `Registry.loadAll(plugin)` duyệt qua list đó và gọi `loadFrom(plugin)` trên từng cái — đây chính là chỗ thực sự đọc `plugin.getDataFolder()/<locationKey>/*.json`, decode từng file qua [`Codec<T>`](Codec-System.md) tương ứng, và báo lỗi theo từng file mà không làm hỏng cả quá trình load:

```java
public void loadFrom(RogueSmpCore plugin) {
    if (locationKey == null || codec == null) return; // registry in-memory không tham gia

    File folder = new File(plugin.getDataFolder(), locationKey);
    // ... với mỗi file *.json:
    DataResult<T> result = codec.decode(json, JsonOps.INSTANCE);
    if (result.isSuccess()) {
        register(id, result.result()); // id = tên file bỏ ".json"
    } else {
        RogueSmpCore.LOGGER.error("Failed to decode [{}] in '{}': {}", id, locationKey, result.error());
    }
}
```

API cốt lõi: `register(String id, U value)`, `get(String id)` (nullable), `getOrThrow(String id)` (ném `IllegalArgumentException` — dùng làm method reference kiểu `Function<String, Codec<?>>` cho [`Codec.dispatch`/`dispatchedMap`](Codec-System.md#polymorphic-dispatch-codecdispatch)), `getAll()` (map bất biến), `clear()`.

## `Registries` — nơi khai báo trung tâm

```java
public class Registries {
    public static final Registry<Codec<? extends ItemComponent>> ITEM_COMPONENT_CODEC = new Registry<>(); // in-memory
    public static final Registry<Codec<? extends SmpEffect>> EFFECT_CODEC = new Registry<>();              // in-memory

    public static final Registry<BaseItem> ITEM = new Registry<>("items", BaseItem.CODEC);                 // data-driven
    public static final Registry<SkinRegistry.SkinData> SKIN_DATA = new Registry<>("skins", SkinRegistry.SkinData.CODEC);

    public static void loadAllData(RogueSmpCore plugin) {
        Registry.loadAll(plugin);
    }

    public static void boostrap(RogueSmpCore plugin) {
        EffectCodecRegistry.bootstrap(); // populate EFFECT_CODEC in-memory
    }
}
```

Có 2 loại `Registry<T>` nằm cạnh nhau ở đây:
- **Bảng tra codec in-memory** (`ITEM_COMPONENT_CODEC`, `EFFECT_CODEC`) — được populate 1 lần lúc bootstrap bởi code (`EffectCodecRegistry.bootstrap()`, static initializer của `ComponentKeys`), dùng thuần túy như bảng tra `String -> Codec<? extends X>` cho [xử lý đa hình](Codec-System.md#polymorphic-dispatch-codecdispatch). Không bao giờ load từ đĩa.
- **Registry data-driven** (`ITEM`, `SKIN_DATA`) — được backing bởi 1 `Codec<T>`, tự động load từ `plugin.getDataFolder()/<locationKey>/*.json` khi `Registries.loadAllData(plugin)` chạy.

Được kết nối từ [`RogueSmpCore`](../src/main/java/com/roguesmp/RogueSmpCore.java):

```java
public void init() {
    ...
    Registries.boostrap(this);      // populate bảng tra codec in-memory trước
    ComponentKeys.loadClass();      // ép codec của item component đăng ký (xem Item System)
    ...
}

public void loadData() {
    Registries.loadAllData(this);   // decode mọi *.json trong mọi Registry data-driven
    ...
}
```

**Thứ tự rất quan trọng**: `Registries.boostrap(this)` phải chạy trước khi bất kỳ thứ gì decode JSON của `SmpEffect`/`ItemComponent`, vì các codec `dispatch`/`dispatchedMap` tra codec của subtype bằng cách tìm trong `EFFECT_CODEC`/`ITEM_COMPONENT_CODEC` ngay lúc decode.

## Thêm 1 registry data-driven mới

```java
// 1. Khai báo trong Registries.java
public static final Registry<MyThing> MY_THING = new Registry<>("my_things", MyThing.CODEC);

// 2. Không cần gì thêm — Registry.loadAll(plugin), được gọi từ Registries.loadAllData,
//    đã tự động phát hiện nó qua list static DATA_REGISTRIES.

// 3. Đọc/dùng ở bất kỳ đâu:
MyThing thing = Registries.MY_THING.get("some_id");
```

Không cần boilerplate `init()`/`getInstance()` — bản thân instance `Registry<T>` *chính là* bề mặt API.

<a id="the-migration-registry-getinstance--registries"></a>
## Quá trình migration: `Registry#getInstance()` → `Registries`

Trước khi `Registries` tồn tại, mỗi tập dữ liệu có 1 class singleton tự viết tay riêng (`ItemRegistry`, `BlockRegistry`, `EntityRegistry`, ...), lặp lại logic load/save cho từng subsystem. Kiểu này đang được thay thế dần bởi `Registries`. [`ItemRegistry`](../src/main/java/com/roguesmp/registry/ItemRegistry.java) cho thấy hình dạng chuyển tiếp mà các lần migration tiếp theo nên theo:

```java
public class ItemRegistry {
    private static ItemRegistry INSTANCE = null;

    private final Map<String, BaseItem> dataMap = Registries.ITEM.getAll(); // <-- giờ backing bởi Registries, không phải Map riêng

    public static void init(RogueSmpCore core) { INSTANCE = new ItemRegistry(core); }

    /**
     * Deprecated, will be removed soon. Change your code to use {@link Registries#ITEM} instead.
     */
    @Deprecated
    public static ItemRegistry getInstance() { return INSTANCE; }
    ...
}
```

Và trong `RogueSmpCore.loadData()`, lệnh load file cũ đã bị comment lại vì `Registries.loadAllData(this)` giờ làm việc đó thay:

```java
public void loadData() {
    Registries.loadAllData(this);
    SkinRegistry.getInstance().loadSkin();
//        ItemRegistry.getInstance().loadFromFile();   // đã được thay thế bởi Registries.ITEM
    ...
}
```

**Khi động vào 1 singleton `*Registry` cũ, hãy làm theo pattern này:**
1. Trỏ storage nội bộ của nó vào field `Registries.<TÊN>` tương ứng, thay vì tự giữ 1 `Map`/tự load file riêng.
2. Đánh dấu `getInstance()` là `@Deprecated` kèm pointer `{@link Registries#<TÊN>}`, giống `ItemRegistry` — đừng xóa hẳn class cho đến khi mọi call site đã được migrate (`getInstance()` có thể vẫn còn expose hành vi, như `saveToFile`, mà chưa được chuyển sang `Registries`).
3. Trong code mới, luôn gọi thẳng `Registries.<TÊN>` — không bao giờ thêm 1 singleton kiểu `getInstance()` mới cho thứ chỉ đơn thuần là 1 tập dữ liệu đã gõ kiểu được load từ JSON. Dành convention singleton (bên dưới) cho các manager có hành vi runtime thật sự, không phải cho registry dữ liệu.

<a id="convention-singleton-manager-khong-lien-quan-nhung-o-khap-noi"></a>
## Convention singleton Manager (không liên quan, nhưng ở khắp nơi)

Hầu hết các subsystem không phải dữ liệu — `PlayerManager`, `IslandManager`, `EffectManager`, `BlockManager`, và (vẫn đang chờ migration) `BlockRegistry`, `EntityRegistry`, `NpcRegistry`, `QuestRegistry`, `SkinRegistry` — theo 1 convention tự viết tay cũ hơn, có trước `Registries` và **không** bị thay thế bởi nó (chúng giữ hành vi runtime thật sự, không chỉ dữ liệu load từ JSON):

```java
public class SkinRegistry {
    private static SkinRegistry INSTANCE;

    public static void init() { INSTANCE = new SkinRegistry(); }

    public static SkinRegistry getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("SkinRegistry is null!");
        return INSTANCE;
    }
    // instance method: loadSkin(), saveSkin(), registerSkin(...), fetchAndRegisterSkin(...), ...
}
```

Constructor private/ngầm định, `INSTANCE` static, `static void init(...)` được gọi 1 lần từ `RogueSmpCore.init()`, `static getInstance()`. **Không có interface/base class dùng chung cho kiểu này** — đừng đi tìm 1 type `Manager<T>` chung; đây là 1 khuôn mẫu lặp lại, không phải abstraction. Thứ tự khởi tạo trong `RogueSmpCore.init()`/`loadData()` rất quan trọng vì dependency phải được khởi tạo trước dependent gọi `getInstance()` lên nó (vd. `EntityManager.init(EntityRegistry.getInstance())` cần `EntityRegistry.init(this)` đã chạy trước đó).

## Lưu ý & lỗi thường gặp

- **Đừng thêm 1 singleton `getInstance()` mới cho dữ liệu JSON thuần túy.** Nếu nó là "1 map đã gõ kiểu chứa các thứ được load từ `*.json`", nó nên là 1 field `Registry<T>` trong `Registries.java`, không phải 1 class tự viết tay mới.
- **`getOrThrow` ném `IllegalArgumentException`, không phải lỗi kiểu `DataResult`.** Nó được thiết kế để dùng làm method reference kiểu `Function<String, Codec<?>>` bên trong `Codec.dispatch`/`dispatchedMap`, nơi thiếu codec của 1 subtype thực sự là lỗi lập trình (component/effect chưa đăng ký), không phải input xấu có thể phục hồi.
- **`Registry<T>` in-memory (constructor không tham số) không bao giờ xuất hiện trong `DATA_REGISTRIES`** và bị `loadFrom`/`loadAll` bỏ qua âm thầm — nó thuần túy là 1 map `String -> T` do bạn tự populate (thường từ 1 lệnh gọi `bootstrap()`/static initializer), dùng như bảng tra thay vì dữ liệu được lưu trữ.
- **`Registries.boostrap(plugin)` phải chạy trước `Registries.loadAllData(plugin)`**, và cả 2 phải chạy trước bất kỳ subsystem nào phụ thuộc vào dữ liệu đã decode gọi `getInstance()`/đọc từ 1 `Registry` — kiểm tra thứ tự trong `RogueSmpCore.init()`/`loadData()` trước khi thêm 1 dependency mới.

---
◀ [Codec System](Codec-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Item System](Item-System.md)
