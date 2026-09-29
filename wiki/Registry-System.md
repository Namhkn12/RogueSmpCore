# Registry System

Package: [`com.roguesmp.registry`](../src/main/java/com/roguesmp/registry)

> ⚠️ **Migration đã hoàn tất phần lớn.** Trước đây mỗi tập dữ liệu (item, entity, npc, quest, skin, block, ...) có 1 class singleton tự viết tay riêng (`ItemRegistry`, `BlockRegistry`, `EntityRegistry`, `NpcRegistry`, `QuestRegistry`, ...), lặp lại logic load/save cho từng subsystem. **Chỉ còn 2 class kiểu này sống sót**: [`ItemRegistry`](../src/main/java/com/roguesmp/registry/ItemRegistry.java) (đã `@Deprecated`, chỉ còn để tương thích ngược) và [`SkinRegistry`](../src/main/java/com/roguesmp/registry/SkinRegistry.java) (vẫn còn hành vi runtime thật — fetch skin từ Mojang). `BlockRegistry`, `EntityRegistry`, `NpcRegistry`, `QuestRegistry` **không còn tồn tại** — dữ liệu của chúng giờ là các field `Registry<T>` phẳng, khai báo thẳng trong [`Registries`](../src/main/java/com/roguesmp/registry/Registries.java) (`Registries.ENTITY`, `Registries.NPC`, `Registries.QUEST`, `Registries.BLOCK_PROPERTIES`). Code mới **luôn** dùng thẳng `Registries.<TÊN>` — không bao giờ tự viết 1 singleton `getInstance()` mới cho dữ liệu JSON thuần túy.

## Hai thứ cùng tên "Registry" — đừng nhầm lẫn

| | Là gì | Ở đâu |
| :--- | :--- | :--- |
| `Registry<T>` | 1 **class** dùng chung, tổng quát. 1 instance = 1 tập hợp entry đã gõ kiểu, có thể được backing bởi JSON hoặc không. | [`Registry.java`](../src/main/java/com/roguesmp/registry/Registry.java) |
| `Registries` | 1 **class chứa** hơn 30 `public static final Registry<T>` — nơi duy nhất mọi instance `Registry<T>` trong plugin được khai báo. | [`Registries.java`](../src/main/java/com/roguesmp/registry/Registries.java) |

## `Registry<T>`

```java
public class Registry<T> {
    private final Map<String, T> entries = new HashMap<>();
    private final Map<String, SmpTag<T>> tags = new HashMap<>();
    private final Map<String, Holder<T>> holders = new HashMap<>();
    private final @Nullable String locationKey;   // tên thư mục con dưới data folder, null = không load từ đĩa
    private final @Nullable Codec<T> codec;        // null = registry in-memory thuần

    public Registry(String locationKey, Codec<T> codec) { ... } // data-driven: load JSON + có thể có tags/
    public Registry(String locationKey) { ... }                  // in-memory nhưng vẫn có thư mục tags/ riêng (vd. Enchants)
    public Registry() { ... }                                    // in-memory thuần, không tags, không load gì cả
    ...
}
```

Mọi `Registry` có `locationKey` khác `null` tự đăng ký vào 1 list static `ALL_REGISTRIES`/`DATA_REGISTRIES`. `Registry.loadAll(plugin)` duyệt qua các registry có `codec` khác `null` và gọi `loadFrom(plugin)` — đọc `plugin.getDataFolder()/<locationKey>/**/*.json`, decode qua [`Codec<T>`](Codec-System.md), báo lỗi theo từng file mà không làm hỏng cả quá trình load.

### Load **đệ quy** theo thư mục con — id không còn chỉ là "tên file"

Khác với trước đây (chỉ đọc phẳng 1 cấp thư mục), `loadFrom` giờ đệ quy vào mọi thư mục con (`loadFromRecursive`), **trừ** 1 thư mục con tên `tags/` (dành riêng cho tag, xem bên dưới). Id của 1 entry là **đường dẫn tương đối** từ thư mục gốc của registry, bỏ đuôi `.json`:

```
items/fire_sword.json          -> id "fire_sword"
items/weapons/fire_sword.json  -> id "weapons/fire_sword"
items/tags/weapons.json        -> KHÔNG phải 1 item — đây là file tag, xem mục Tag bên dưới
```

Dùng subfolder để nhóm nội dung liên quan (vd. `items/weapons/`, `items/armor/`) mà không ảnh hưởng gì đến cách entry được tham chiếu ở nơi khác — miễn tham chiếu dùng đúng id đầy đủ gồm cả path con.

API cốt lõi: `register(String id, U value)`, `get(String id)` (nullable), `getOrDefault(String id, T fallback)`, `getOrThrow(String id)` (ném `IllegalArgumentException` — dùng làm method reference kiểu `Function<String, Codec<?>>` cho [`Codec.dispatch`/`dispatchedMap`](Codec-System.md#polymorphic-dispatch-codecdispatch)), `getAll()` (map bất biến), `clear()`.

<a id="holdert--tham-chiếu-ổn-định-qua-id"></a>
## `Holder<T>` — tham chiếu ổn định qua id

Khi 1 field JSON của registry *này* cần trỏ tới 1 entry của registry *khác* (vd. yêu cầu nâng cấp ability cần N cục item, `BlockProperties` liệt kê tool được phép dùng để đào), đừng decode bằng `Codec.STRING` trần rồi tự tra `Registries.X.get(id)`. Dùng [`Holder<T>`](../src/main/java/com/roguesmp/registry/Holder.java) — 1 tham chiếu **giữ nguyên identity theo id**, mô phỏng `Holder<T>` của chính Minecraft:

```java
Holder<BaseItem> ref = Registries.ITEM.getHolder("ruby_shard"); // có thể "chưa bind" nếu chưa entry nào đăng ký id này
ref.isBound();  // false nếu chưa
ref.value();    // ném IllegalStateException nếu gọi lúc chưa bind — luôn check isBound() trước nếu có thể null
```

- Gọi `getHolder(id)` nhiều lần với cùng 1 id luôn trả về **cùng 1 instance** `Holder`.
- Khi `register(id, value)` được gọi sau đó cho đúng id ấy, **mọi** `Holder` đang tồn tại cho id đó tự động được bind — không quan trọng `getHolder` hay `register` cái nào chạy trước, tham chiếu luôn đúng bất kể thứ tự load giữa các registry.
- `Registry#clear()` (dùng khi reload) không hủy các `Holder` đã phát ra — nó `unbind()` chúng, để lần load lại tự bind lại đúng instance cũ, không phá code đang giữ tham chiếu.
- `Holder` còn giữ ngược 1 `Set<String> tagIds` — set tag nào (của registry chủ) hiện đang chứa entry này, được đồng bộ mỗi lần tag load lại.

Codec sẵn dùng để decode 1 field JSON thành `Holder<T>`:

```java
public static <T> Codec<Holder<T>> referenceCodec(Supplier<Registry<T>> registrySupplier)
```

Nhận `Supplier<Registry<T>>` (không phải `Registry<T>` trực tiếp) để tránh lỗi thứ tự khởi tạo static field trong `Registries` — chỉ thực sự tra registry lúc decode. Ví dụ thật, [`ItemRequirement`](../src/main/java/com/roguesmp/player/ability/upgrade/ItemRequirement.java):

```java
public static final Codec<ItemRequirement> CODEC = Codec.composite(
        Registry.referenceCodec(() -> Registries.ITEM).fieldOf("item_id").forGetter(ItemRequirement::getRequiredItemHolder),
        Codec.INT.fieldOf("amount").forGetter(ItemRequirement::getAmount),
        ItemRequirement::new
);
```

`getRequiredItem()` trả `null` nếu `!requiredItem.isBound()` (id gõ sai/không tồn tại) thay vì ném lỗi — tra cứu id sai không nên crash lúc load, chỉ nên vô hiệu hóa đúng chỗ đó. Để **bắt được** id sai/gõ nhầm đó ngay lúc khởi động thay vì âm thầm trả `null` mãi mãi, xem `validateAllHolders()` ngay bên dưới.

`Registry.validateAllHolders()` (static, không tham số): sau khi mọi registry đã load xong, duyệt toàn bộ, tìm những `Holder` đã được `getHolder(id)` xin nhưng chưa bao giờ có `register(id, ...)` khớp, log:

```
Unresolved reference '<id>' in registry '<locationKey>' - nothing is registered under this id
```

Đây **chỉ là warning log, không ném exception** — 1 id gõ sai không làm sập server lúc khởi động, nhưng sẽ hiện ngay trong log để dev/designer sửa. Chạy 1 lần từ `Registries.loadAllData(plugin)` (ngay sau khi mọi entry + tag đã load), và lại từ `Registry.reloadOne(...)` mỗi lần reload riêng 1 registry.

<a id="tag-system"></a>
## Tag — nhóm nhiều entry lại dưới 1 id

Mọi registry data-driven (`Registry(locationKey, codec)`) — và cả registry in-memory nhưng vẫn muốn có tag (`Registry(locationKey)`, không codec, vd. `Enchants`) — tự động load thêm `<locationKey>/tags/*.json`. Id của 1 tag là **tên file** (không tính path con như entry thường). Shape JSON: **1 mảng chuỗi trần**, không phải object `{"values": [...]}` kiểu Minecraft:

```json
// items/tags/weapons.json
["iron_sword", "steel_dagger", "flame_bow", "#legendary_swords"]
```

Entry bắt đầu bằng `#` nghĩa là "gộp thêm mọi phần tử của tag khác" (đệ quy, tự phát hiện vòng lặp tham chiếu — log lỗi thay vì treo). Id không tồn tại trong registry chủ → log warning, bỏ qua entry đó, không fail cả tag.

Vài tag là **built-in** — khai báo cứng trong code (`tag/Tags.java`), luôn tồn tại (rỗng nếu không có file JSON tương ứng) và giữ nguyên identity qua các lần reload:

```java
public static final SmpTag<BaseItem> WEAPONS = builtIn("weapons", () -> Registries.ITEM);
public static final SmpTag<BaseEntity> ELITE = builtIn("elite", () -> Registries.ENTITY);
public static final SmpTag<BaseEntity> BOSS = builtIn("boss", () -> Registries.ENTITY);
public static final SmpTag<BaseEntity> ANGELIC = builtIn("angelic", () -> Registries.ENTITY);
public static final SmpTag<BaseEntity> FRIENDLY = builtIn("friendly", () -> Registries.ENTITY);
public static final SmpTag<Quest> DAILY_EASY_QUEST = builtIn("daily_easy_quest", () -> Registries.QUEST);
public static final SmpTag<Quest> DAILY_MEDIUM_QUEST = builtIn("daily_medium_quest", () -> Registries.QUEST);
public static final SmpTag<Quest> DAILY_HARD_QUEST = builtIn("daily_hard_quest", () -> Registries.QUEST);
```

`Tags.bootstrap()` phải chạy **trước** bất kỳ tag nào được load từ đĩa (xem thứ tự trong `Registries.boostrap` bên dưới), nếu không những entry built-in trên sẽ không tồn tại để nhận dữ liệu JSON đè lên. Ứng dụng thực tế đã thấy: [`PlayerClass.allowedWeapons`](Player-Class-System.md) là 1 `Set<String>` id-của-tag-trên-`Registries.ITEM` (vd. `"weapons"`), không phải id item trực tiếp — 1 class được phép dùng "mọi thứ nằm trong tag này."

API sửa tag từ code: `Registry.saveTag(plugin, tagId, List<String> rawEntries)` (ghi file + reload ngay), `deleteTag(plugin, tagId)`.

## `Registries` — nơi khai báo trung tâm

[`Registries.java`](../src/main/java/com/roguesmp/registry/Registries.java) hiện có **hơn 30** field `Registry<T>`, chia làm 2 nhóm:

**Bảng tra codec in-memory** (dùng cho [xử lý đa hình](Codec-System.md#polymorphic-dispatch-codecdispatch), không bao giờ load từ đĩa): `ITEM_COMPONENT_CODEC`, `ENTITY_COMPONENT_CODEC`, `EFFECT_CODEC`, `ITEM_ABILITY_CODEC`, `QUEST_OBJECTIVE_CODEC`, `QUEST_REQUIREMENT_CODEC`, `QUEST_REWARD_CODEC`, `OBJECTIVE_PROGRESS_CODEC`, `ABILITY_UPGRADE_REQUIREMENT_CODEC`, `NPC_ACTION_CODEC`, `LOOT_CONDITION_CODEC`, `LOOT_FUNCTION_CODEC`, `LOOT_ENTRY_CODEC`, `CRAFTING_RECIPE_CODEC`.

**Registry data-driven** (backing bởi 1 `Codec<T>`, tự load từ `<dataFolder>/<locationKey>/`):

| Field | Kiểu | Thư mục | Trang wiki |
| :--- | :--- | :--- | :--- |
| `ITEM` | `Registry<BaseItem>` | `items/` | [Item System](Item-System.md) |
| `ENTITY` | `Registry<BaseEntity>` | `entities/` | [Entity, Boss & Spell System](Entity-Boss-Spell-System.md) |
| `NPC` | `Registry<BaseNpc>` | `npcs/` | [NPC System](Npc-System.md) |
| `QUEST` | `Registry<Quest>` | `quests/` | [Quest System](Quest-System.md) |
| `LOOT_TABLE` | `Registry<LootTable>` | `loottable/` | [Loot System](Loot-System.md) |
| `CRAFTING_RECIPE` | `Registry<CraftingRecipe>` | `crafting_recipes/` | [Crafting System](Crafting-System.md) |
| `BLOCK_PROPERTIES` | `Registry<BlockProperties>` | `blocks/` | [Block System](Block-System.md) |
| `ITEM_TYPE` | `Registry<ItemType>` | `item_types/` | [Item System](Item-System.md) |
| `PLAYER_CLASS` | `Registry<PlayerClass>` | `classes/` | [Player Class System](Player-Class-System.md) |
| `ABILITY_CONFIG` | `Registry<AbilityConfig>` | `ability_info/` | [Player Ability System](Player-Ability-System.md) |
| `SKIN_DATA` | `Registry<SkinRegistry.SkinData>` | `skins/` | — |

Vài registry còn lại là bảng tra **in-memory nhưng code-populated qua cùng cơ chế bootstrap** (`ENCHANTS`, `GLYPH`, `ENTITY_SPELL`, `ENTITY_FACTORY`, `ABILITY`, `BLOCK_TYPE`, `TRIGGER_OPTION`, `NPC_GUI_OPEN_ACTION`) — không có file JSON tương ứng (trừ `ENCHANTS`, chỉ có `enchants/tags/*.json` vì nó vẫn muốn hỗ trợ tag dù bản thân là enum cứng).

<a id="cơ-chế-bootstrap"></a>
### Cơ chế bootstrap — `register(Bootstrapper<T>)`

Không còn cảnh mỗi registry in-memory gọi tay `XBootstrap.bootstrap()` riêng lẻ trong `Registries.boostrap()`. Giờ khai báo field xong là tự động đăng ký vào 1 danh sách chờ:

```java
public class Registries {
    private static final List<Consumer<RogueSmpCore>> BOOTSTRAPPERS = new ArrayList<>();

    public static final Registry<Codec<? extends ItemComponent>> ITEM_COMPONENT_CODEC =
            register(registry -> ItemComponentKeys.loadClass());
    public static final Registry<AbilityInfo<? extends Ability>> ABILITY =
            register(registry -> AbilityInfos.loadClass());
    // ... hơn 15 field khác theo cùng khuôn

    public static void boostrap(RogueSmpCore plugin) {
        BOOTSTRAPPERS.forEach(consumer -> consumer.accept(plugin)); // chạy loadClass() của mọi field register(...) ở trên
        Tags.bootstrap(); // built-in tag phải khai báo xong trước khi bất kỳ registry nào load tag của nó
    }

    private static <T> Registry<T> register(Bootstrapper<T> bootstrapper) {
        Registry<T> registry = new Registry<>();          // in-memory thuần
        BOOTSTRAPPERS.add(plugin -> bootstrapper.run(registry));
        return registry;
    }
    // + overload register(String key, Bootstrapper) cho registry in-memory nhưng vẫn có thư mục tags/
}
```

`loadAllData(plugin)` giờ làm 3 việc theo đúng thứ tự, không chỉ 1:

```java
public static void loadAllData(RogueSmpCore plugin) {
    Registry.loadAll(plugin);       // decode mọi *.json (đệ quy) trong mọi registry data-driven
    Registry.loadAllTags(plugin);   // load tags/*.json — PHẢI chạy sau, vì tag resolve theo entry đã có
    Registry.validateAllHolders();  // bắt Holder tham chiếu id không tồn tại — chỉ log, không throw
}
```

<a id="hot-reload--registriesreloadalldata--registryreloadone"></a>
## Hot reload — `Registries.reloadAllData` / `Registry.reloadOne`

```java
public static void reloadAllData(RogueSmpCore plugin) {
    Registry.clearAll();     // clear() mọi registry — Holder tự unbind, không mất tham chiếu
    loadAllData(plugin);     // rồi load lại y hệt lúc khởi động
}
```

`Registry.reloadOne(String locationKey, RogueSmpCore plugin)` làm y vậy nhưng chỉ cho **1** registry (theo `locationKey`) — dùng khi chỉ muốn nạp lại 1 loại nội dung (vd. chỉ sửa item, không muốn động vào dungeon đang chạy). `RogueSmpCore.reloadData()`/`reloadRegistry(String)` expose 2 cái này ra 1 command vận hành (`ReloadCommand`), và còn tự làm thêm bước dọn dẹp phụ thuộc khi cần — reload `crafting_recipes` tự dựng lại `CraftingManager`, reload `classes` tự đồng bộ lại roster ability của mọi player đang online (`SmpPlayer.syncClassRoster()`).

## Được kết nối từ [`RogueSmpCore`](../src/main/java/com/roguesmp/RogueSmpCore.java)

```java
public void init() {
    ...
    Registries.boostrap(this);          // chạy mọi loadClass() đã đăng ký + Tags.bootstrap()
    ItemComponentKeys.loadClass();      // (đã chạy như 1 phần của Registries.boostrap ở trên, gọi lại vô hại)
    EntityComponentKeys.loadClass();    // mirror của ItemComponentKeys, cho Map<String, EntityComponent> của BaseEntity
    ...
}

public void loadData() {
    Registries.loadAllData(this);       // load + validate mọi *.json
    CraftingManager.init();             // dựng Trie công thức — cần Registries.ITEM đã load xong
    VanillaRecipeReplacer.replaceAll();
    ...
}
```

**Thứ tự rất quan trọng**: `boostrap()` phải chạy trước `loadAllData()`, vì các codec `dispatch`/`dispatchedMap` tra codec của subtype bằng cách tìm trong các bảng `*_CODEC` in-memory ngay lúc decode — nếu bảng đó còn rỗng, decode sẽ fail cho mọi entry dùng kiểu đa hình.

## Thêm 1 registry data-driven mới

```java
// 1. Khai báo trong Registries.java
public static final Registry<MyThing> MY_THING = new Registry<>("my_things", MyThing.CODEC);

// 2. Không cần gì thêm — Registry.loadAll(plugin), được gọi từ Registries.loadAllData,
//    đã tự động phát hiện nó, load đệ quy thư mục con, và load tags/ nếu có.

// 3. Đọc/dùng ở bất kỳ đâu:
MyThing thing = Registries.MY_THING.get("some_id");
Holder<MyThing> ref = Registries.MY_THING.getHolder("some_id"); // nếu chỗ khác cần tham chiếu ổn định theo id
```

Không cần boilerplate `init()`/`getInstance()` — bản thân instance `Registry<T>` *chính là* bề mặt API.

<a id="the-migration-registry-getinstance--registries"></a>
## 2 class singleton còn sót lại

[`ItemRegistry`](../src/main/java/com/roguesmp/registry/ItemRegistry.java) — hình dạng chuyển tiếp, đã `@Deprecated`:

```java
public class ItemRegistry {
    private static ItemRegistry INSTANCE = null;
    private final Map<String, BaseItem> dataMap = Registries.ITEM.getAll(); // backing bởi Registries, không phải Map riêng

    public static void init(RogueSmpCore core) { INSTANCE = new ItemRegistry(core); }

    /** @deprecated dùng {@link Registries#ITEM} */
    @Deprecated
    public static ItemRegistry getInstance() { return INSTANCE; }
}
```

[`SkinRegistry`](../src/main/java/com/roguesmp/registry/SkinRegistry.java) — **không** deprecated, vì nó có hành vi runtime thật (fetch skin từ Mojang API, cache) không chỉ là dữ liệu tĩnh; phần dữ liệu tĩnh của nó (`SkinData`) đã tách riêng thành `Registries.SKIN_DATA`, còn bản thân `SkinRegistry` vẫn giữ pattern singleton `getInstance()` cho phần hành vi.

**Khi động vào 1 trong 2 singleton này:** không thêm hành vi mới vào `getInstance()`, chuyển hẳn sang `Registries.<TÊN>` cho code mới. Không bao giờ tạo 1 singleton `getInstance()` mới cho thứ chỉ đơn thuần là "1 map đã gõ kiểu được load từ JSON."

## Lưu ý & lỗi thường gặp

- **Đừng thêm 1 singleton `getInstance()` mới cho dữ liệu JSON thuần túy.** Nếu nó là "1 map đã gõ kiểu chứa các thứ được load từ `*.json`", nó nên là 1 field `Registry<T>` trong `Registries.java`.
- **`getOrThrow` ném `IllegalArgumentException`**, dùng làm method reference `Function<String, Codec<?>>` bên trong `Codec.dispatch`/`dispatchedMap` — thiếu codec của 1 subtype ở đó là lỗi lập trình (chưa đăng ký), không phải input xấu có thể phục hồi.
- **Id của 1 entry giờ có thể chứa `/`** vì load đệ quy theo thư mục con — đừng giả định id luôn bằng tên file trần khi viết code mới đọc/ghi theo id.
- **Thư mục con tên `tags` bị load đệ quy bỏ qua hoàn toàn** — không đặt file entry thường trong 1 thư mục tên `tags/`, nó sẽ không bao giờ được nạp như entry.
- **`Holder` không bao giờ throw lúc decode** dù id không tồn tại — nó chỉ "chưa bind." Luôn `isBound()` trước khi `value()` ở bất kỳ đâu id có thể đến từ nội dung do designer tự viết tay; đừng chỉ dựa vào `validateAllHolders()` (chỉ log) để đảm bảo tính đúng đắn lúc runtime.
- **`Registries.boostrap(plugin)` phải chạy trước `Registries.loadAllData(plugin)`**, và `loadAllTags`/`validateAllHolders` phải chạy sau `loadAll` — kiểm tra thứ tự trong `RogueSmpCore.init()`/`loadData()` trước khi thêm 1 dependency mới.
- **`clearAll()`/`reloadAllData()` không phá `Holder` đang tồn tại** — chúng chỉ unbind rồi bind lại sau khi load xong, nên code giữ tham chiếu `Holder` từ trước 1 lần reload vẫn tiếp tục đúng sau reload, miễn nó gọi `isBound()`/`value()` sau khi reload hoàn tất chứ không phải giữa chừng.

---
◀ [Codec System](Codec-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Item System](Item-System.md)
