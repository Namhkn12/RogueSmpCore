# Wiki RogueSmpCore

RogueSmpCore là plugin custom-content cho server Minecraft **Paper 1.21.11** (`com.roguesmp`): 1 tầng framework item / attribute / entity / ability / dungeon / quest / npc / loot / crafting / block tùy chỉnh trên nền Bukkit. Java 21, build bằng Gradle, không có unit test — kiểm chứng bằng cách chạy trên server Paper thật (`./gradlew runServer`).

Phần lớn nội dung game là **file JSON** do designer viết, nạp lúc khởi động (và reload nóng được). Wiki chia làm 2 phần:

## 1. JSON Reference — cho designer & dev viết nội dung

Mỗi trang = 1 thư mục dữ liệu: **mỗi key làm gì, kiểu gì, bắt buộc hay mặc định bao nhiêu, JSON có cấu trúc thế nào**, kèm ví dụ đầy đủ. Bắt đầu từ trang tổng quan.

| Trang | Thư mục | Nội dung |
| :--- | :--- | :--- |
| **[JSON-Overview](JSON-Overview.md)** | (tất cả) | Bản đồ thư mục, quy tắc chung: id = đường dẫn, kiểu giá trị, MiniMessage, tag, tham chiếu chéo, lỗi, reload. **Đọc trước.** |
| [JSON-Items](JSON-Items.md) | `items/`, `item_types/`, `skins/` | 24 component, attribute, enchant, effect, item ability |
| [JSON-Entities](JSON-Entities.md) | `entities/` | Mob/boss: component, spell, boss bar, loot |
| [JSON-Abilities](JSON-Abilities.md) | `ability_info/` | Chỉ số theo level, phím kích hoạt, chi phí nâng cấp, bảng ability có sẵn |
| [JSON-Classes](JSON-Classes.md) | `classes/` | Lớp nhân vật: roster ability, giới hạn vũ khí |
| [JSON-Quests](JSON-Quests.md) | `quests/` | Quest, objective, reward, quest hằng ngày |
| [JSON-Npcs](JSON-Npcs.md) | `npcs/` | NPC và action khi click |
| [JSON-Loot](JSON-Loot.md) | `loottable/` | Pool, entry, condition, thuật toán roll |
| [JSON-Crafting](JSON-Crafting.md) | `crafting_recipes/` | `shaped`, `shapeless`, `fusion` |
| [JSON-Blocks](JSON-Blocks.md) | `blocks/`, `fuel_types/` | Block, máy phát (generator), module, nhiên liệu, altar |
| [JSON-Dungeons](JSON-Dungeons.md) | `dungeon_v2/template/` | Dungeon, phòng, objective, room event, spawner (hệ riêng, dùng Gson) |

## 2. Kiến trúc — cho dev

Giải thích **vì sao** hệ thống chạy như vậy và cách mở rộng bằng code.

1. **[Codec System](Codec-System.md)** — tầng serialize độc lập định dạng (`Codec`, `MapCodec`, `DynamicOps`, `DataResult`), lấy cảm hứng từ DataFixerUpper của Mojang. Hầu hết việc load JSON dựa trên nó.
2. **[Registry System](Registry-System.md)** — `Registry<T>` (load đệ quy, tag, hot reload), `Registries` gom hơn 30 registry, `Holder<T>` (tham chiếu ổn định theo id).
3. **[Item System](Item-System.md)** — `BaseItem` (template) / `SmpItem` (wrapper runtime), `ItemComponent`, pipeline `generateItemStack`, `ItemAbility`.
4. **[Attribute System](Attribute-System.md)** — `SmpAttribute`, enum `Attributes`, attribute wrap vanilla vs. custom theo gameplay-event.
5. **[GUI Framework](GUI-Framework.md)** — `BaseGui` + `GuiListener`, `ReactiveGui<S>`.
6. **[Entity, Boss & Spell System](Entity-Boss-Spell-System.md)** — `BaseEntity`/`SmpEntity`, `EntityComponent`, `SpellComponent`, `PhaseComponent`.
7. **[Player Ability System](Player-Ability-System.md)** — `Ability`, `AbilityInfo` (code) tách khỏi `AbilityConfig` (JSON), `AbilityLoadout`, trigger & capture.
8. **[Dungeon System](Dungeon-System.md)** — bảy tầng phân lớp nghiêm ngặt (definition → runtime → repository → manager → service → controller → actor).
9. **[Fx System](Fx-System.md)** — hiệu ứng particle/display thuần hiển thị: `FxShape` + `FxMotion` + `FxRenderer`. Không JSON-hóa được.

Mọi thứ khởi động từ [`RogueSmpCore`](../src/main/java/com/roguesmp/RogueSmpCore.java): `init()` (khởi tạo singleton + bootstrap các bảng tra codec), `loadData()` (decode mọi `*.json` qua Registry), `initListeners()`, `initCommands()`.

## Các convention xuyên suốt

- **Component + Codec** là khuôn mẫu chính cho nội dung mở rộng được: `ItemComponent`, `EntityComponent`, `ItemAbility`, và các nhóm đa hình dispatch trên `"type"` (quest, npc action, loot, crafting recipe, upgrade requirement, spell, block). Hook gameplay-event (`SmpAttribute`, `Ability`, `Spell`) theo khuôn interface/abstract class với hook mặc định no-op. Khi thêm điểm mở rộng mới, theo 1 trong 2 khuôn này.
- **Singleton `getInstance()` kiểu cũ** chỉ còn ở `ItemRegistry` (deprecated) và `SkinRegistry`; dữ liệu JSON thuần dùng field `Registry<T>` trong [`Registries`](Registry-System.md).
- **`Holder<T>` / `Registry.referenceCodec`** để tham chiếu entry của registry khác theo id; **tag** (`<thư mục>/tags/*.json`) để nhóm entry — xem [JSON-Overview](JSON-Overview.md#tag).
- **PDC key** đi qua `constant/Keys.java` (namespace `"smp"`); **component key** đi qua [`ItemComponentKeys`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java) / [`EntityComponentKeys`](../src/main/java/com/roguesmp/entity/component/EntityComponentKeys.java).
- **GUI** đi qua `BaseGui` + `GuiListener` — không đăng ký thêm listener `InventoryClickEvent` thô.
- **Fx thuần hiển thị** — không import gameplay logic (`DamageUtils`...) vào `com.roguesmp.fx`.

## Build & chạy

```sh
./gradlew build          # compile + assemble
./gradlew shadowJar      # fat jar đã relocate (com.roguesmp.libs.glowingentities)
./gradlew runServer      # chạy server Paper 1.21 local với plugin đã load sẵn
```

Sandbox của `runServer` **không** tự cài các plugin `depend` (CommandAPI, FastAsyncWorldEdit, TAB, PlaceholderAPI, Multiverse-Core, WorldGuard) — bỏ chúng vào `plugins/` của server test để chạy thử các tính năng phụ thuộc cứng.
