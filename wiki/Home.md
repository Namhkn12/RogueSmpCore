# Wiki RogueSmpCore

RogueSmpCore là plugin custom-content cho server Minecraft **Paper 1.21.11** (`com.roguesmp`). Plugin xây dựng một tầng framework item/attribute/entity/ability/dungeon/quest/npc/loot/crafting/block tùy chỉnh trên nền Bukkit gốc. Viết bằng Java 21, build bằng Gradle, không có bộ unit test — việc kiểm chứng được thực hiện bằng cách chạy/attach vào một server Paper thật (`./gradlew runServer`).

Mọi thứ đều khởi động từ [`RogueSmpCore`](../src/main/java/com/roguesmp/RogueSmpCore.java) (entrypoint `JavaPlugin`). `onEnable()` chạy theo thứ tự: `init()` (khởi tạo mọi manager/registry singleton + bootstrap mọi bảng tra codec in-memory), `loadData()` (decode mọi `*.json` qua [Registry System](Registry-System.md)), `initListeners()`, `initCommands()`. Hãy đọc file này đầu tiên khi cần lần theo cách một subsystem bất kỳ được kết nối — đây là nơi duy nhất mọi singleton cấp cao được khởi tạo và sắp xếp thứ tự.

> Phần lớn nội dung game (item, entity/boss, ability, dungeon, quest, npc, loot, công thức chế tạo, block) đều là **file JSON** dưới `plugin.getDataFolder()/`, load qua [Registry System](Registry-System.md) + [Codec System](Codec-System.md). Mỗi trang dưới đây đều có ít nhất 1 ví dụ JSON đầy đủ, dùng được ngay cho designer viết nội dung — không chỉ giải thích kiến trúc cho dev.

## Danh sách trang

**Nền tảng — đọc trước, mọi trang khác đều dựa vào 2 trang này:**

1. **[Codec System](Codec-System.md)** — tầng serialize độc lập định dạng (`Codec`, `MapCodec`, `DynamicOps`, `DataResult`), lấy cảm hứng từ DataFixerUpper của Mojang. Hầu hết việc load JSON của các subsystem khác đều dựa trên nó.
2. **[Registry System](Registry-System.md)** — class `Registry<T>` dùng chung (load đệ quy theo thư mục con, tag, hot reload), `Registries` gom hơn 30 registry lại 1 chỗ, `Holder<T>` (tham chiếu ổn định theo id qua các registry khác nhau), và convention singleton Manager (khác, không liên quan) vẫn còn sống ở vài chỗ.

**Nội dung gameplay do designer viết JSON, theo tầng phụ thuộc:**

3. **[Item System](Item-System.md)** — mô hình 2 tầng `BaseItem`/`SmpItem`, 23 component đã đăng ký (tên, độ bền, attribute, gem, potion, ability thụ động, ...), kèm bảng tham khảo đầy đủ + ví dụ JSON cho từng loại vũ khí/trang bị/vật phẩm ăn được.
4. **[Attribute System](Attribute-System.md)** — `SmpAttribute`, enum `Attributes` (34 hằng số, không có registry riêng), attribute wrap vanilla vs. attribute custom theo gameplay-event.
5. **[GUI Framework](GUI-Framework.md)** — `BaseGui` + `GuiListener` xử lý click tập trung; `ReactiveGui<S>` cho GUI cần tự vẽ lại theo state (máy chế tạo, ...).
6. **[Entity, Boss & Spell System](Entity-Boss-Spell-System.md)** — `BaseEntity` giờ dùng component + Codec (mirror Item System), `SpellComponent`/`PhaseComponent` cho casting và chuyển pha boss theo ngưỡng máu.
7. **[Player Ability System](Player-Ability-System.md)** — `AbilityInfo` (code) tách khỏi `AbilityConfig` (JSON: scaling/trigger/mô tả/chi phí nâng cấp), `AbilityTrigger` + `TriggerOption`, `UpgradeRequirement`.
8. **[Player Class System](Player-Class-System.md)** — `PlayerClass`: roster ability + hạn chế vũ khí theo tag, quyết định ability nào 1 player được phép trang bị.
9. **[Dungeon System](Dungeon-System.md)** — subsystem lớn nhất: bảy tầng phân lớp nghiêm ngặt (definition → runtime → repository → manager → service → controller → actor), cộng ví dụ JSON cho room/objective/spawner/room-event.
10. **[Quest System](Quest-System.md)** — `Quest` (requirement/objective/reward đa hình), độc lập hoàn toàn với dungeon.
11. **[NPC System](Npc-System.md)** — `BaseNpc` + `NpcAction` đa hình (`run_command`/`open_gui`).
12. **[Loot System](Loot-System.md)** — `LootTable`/`LootPool`/`LootEntry`/`LootCondition` kiểu Minecraft nhưng shape JSON riêng, dùng bởi cả dungeon lẫn entity.
13. **[Crafting System](Crafting-System.md)** — `CraftingRecipe` (`shaped`/`shapeless`/`fusion`), index qua `CraftingManager`.
14. **[Block System](Block-System.md)** — block tùy chỉnh dựng từ `ItemDisplay`, `BlockProperties` (JSON) + `BlockType`/`SmpBlock` (code) nối nhau bằng id — subsystem vừa rework gần đây nhất.
15. **[Fx System](Fx-System.md)** — hiệu ứng particle/block/item-display: `FxShape` + `FxMotion` + `FxRenderer`, gộp thành `FxPart`/`FxEffect` qua `FxEngine`. Thuần code, không JSON-hóa được — hit-detection đi qua accessor đọc-only (`currentTransform()`), không nằm trong package này.

## Các convention xuyên suốt (áp dụng ở mọi nơi, không chỉ 1 trang)

- **Component + Codec** là khuôn mẫu chính cho mọi nội dung đa hình/mở rộng được: `ItemComponent` (Item), `EntityComponent` (Entity), `ItemAbility` (Item), `Ability`/`SmpAttribute`/`Spell` (gameplay-event hook, không phải Codec component), và các nhóm đa hình dispatch-trên-`"type"` (Quest objective/requirement/reward, NPC action, Loot entry/condition, Crafting recipe, Ability upgrade requirement). Khi thêm 1 điểm mở rộng cross-cutting mới, hãy theo 1 trong 2 khuôn này thay vì tự nghĩ ra cơ chế khác.
- **Convention singleton Registry/Manager cũ** (constructor private, `INSTANCE` static, `init(...)`, `getInstance()`) **chỉ còn sót lại ở `ItemRegistry` (deprecated) và `SkinRegistry`** — mọi subsystem dữ liệu khác (`Registries.ENTITY`, `Registries.NPC`, `Registries.QUEST`, `Registries.BLOCK_PROPERTIES`, ...) đã chuyển hẳn sang field `Registry<T>` phẳng trong [`Registries`](Registry-System.md). Đừng tạo 1 singleton `getInstance()` mới cho dữ liệu JSON thuần túy.
- **`Holder<T>`** ([Registry System](Registry-System.md#holdert--tham-chiếu-ổn-định-qua-id)) là cách chuẩn để 1 field JSON tham chiếu sang entry của 1 registry khác theo id (item cần cho upgrade, tool được phép đào 1 block, ...) — dùng `Registry.referenceCodec(...)`, đừng tự `Codec.STRING` trần rồi tra tay.
- **Tag** ([Registry System](Registry-System.md#tag-system)) cho phép nhóm nhiều entry của cùng 1 registry dưới 1 id (`<locationKey>/tags/*.json`, mảng chuỗi trần, hỗ trợ `#tham-chiếu` tag khác) — dùng cho vũ khí lớp nhân vật được phép cầm, quest hằng ngày theo độ khó, entity boss/elite, ...
- **PDC key** đi qua `constant/Keys.java` dưới namespace dùng chung `"smp"` — không tự tạo `NamespacedKey` ở nơi khác.
- **Định danh/codec của item component** đi qua [`item/component/ItemComponentKeys.java`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java) (⚠️ không còn ở `constant/ComponentKeys.java`); entity component có 1 hệ mirror y hệt ở [`entity/component/EntityComponentKeys.java`](../src/main/java/com/roguesmp/entity/component/EntityComponentKeys.java) — xem [Item System](Item-System.md)/[Entity System](Entity-Boss-Spell-System.md).
- **GUI** đi qua `BaseGui` + `GuiListener` — không đăng ký thêm listener `InventoryClickEvent` thô; xem [GUI Framework](GUI-Framework.md).
- **Fx (`com.roguesmp.fx`) thuần hiển thị** — không import `DamageUtils`/`DamageEvent` hay bất kỳ gameplay logic nào vào package này. Code cần phản ứng theo vị trí 1 effect/part đọc `currentTransform()` (accessor đọc-only) từ vòng lặp tick của riêng mình; xem [Fx System](Fx-System.md#pattern-đọc-vị-trí-hiện-tại--read-only-accessor).

## Build & chạy

```sh
./gradlew build          # compile + assemble
./gradlew shadowJar      # tạo fat jar đã relocate (com.roguesmp.libs.glowingentities)
./gradlew runServer      # chạy 1 server Paper 1.21 local với plugin này đã load sẵn
```

Sandbox của `runServer` **không** tự động cài các plugin `depend` lúc runtime (CommandAPI, FastAsyncWorldEdit, TAB, PlaceholderAPI, Multiverse-Core, WorldGuard) — cần tự tay bỏ chúng vào thư mục `plugins/` của server test để chạy thử các tính năng phụ thuộc cứng vào các plugin đó.

## Nội dung có thể đổi lúc runtime (hot reload)

`Registries.reloadAllData(plugin)` (clear toàn bộ + load lại) và `Registry.reloadOne(locationKey, plugin)` (chỉ 1 registry) cho phép sửa file JSON rồi nạp lại mà không cần restart server — expose qua `RogueSmpCore.reloadData()`/`reloadRegistry(String)` và 1 lệnh vận hành (`ReloadCommand`). Xem [Registry System → Hot reload](Registry-System.md#hot-reload--registriesreloadalldata--registryreloadone) để biết chi tiết + những gì tự động đồng bộ theo (crafting recipe re-index, roster ability của player online).
