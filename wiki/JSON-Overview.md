# JSON Reference — Tổng quan

Phần lớn nội dung game (item, mob/boss, ability, class, quest, NPC, loot, công thức, block, dungeon) là **file JSON** do designer viết, nằm dưới thư mục dữ liệu của plugin (`plugins/RogueSmpCore/`). Các trang `JSON-*` mô tả: **mỗi thư mục chứa gì, mỗi key làm gì, JSON có cấu trúc thế nào**, kèm ví dụ đầy đủ. Trang này gom những quy tắc chung áp dụng cho mọi file — đọc 1 lần trước khi viết nội dung.

> Các trang kiến trúc (Codec System, Registry System, Item System, ...) giải thích **vì sao** hệ thống chạy như vậy, dành cho dev. Các trang `JSON-*` trả lời **viết gì vào file**.

## Bản đồ thư mục

Đường dẫn dưới đây tính từ thư mục dữ liệu plugin. "Registry" nghĩa là load qua [Registry System](Registry-System.md) + [Codec System](Codec-System.md); riêng dungeon dùng Gson + repository riêng nên quy tắc khác (xem [JSON-Dungeons](JSON-Dungeons.md)).

| Thư mục / file | Chứa gì | Trang |
| :--- | :--- | :--- |
| `items/` | Định nghĩa item | [JSON-Items](JSON-Items.md) |
| `item_types/` | Loại item (nhãn "Phân loại", điều kiện dụng cụ đào block) | [JSON-Items](JSON-Items.md#item_types--loại-item) |
| `skins/` | Dữ liệu skin (đầu người, NPC) | [JSON-Items](JSON-Items.md#skins--dữ-liệu-skin) |
| `entities/` | Mob/boss | [JSON-Entities](JSON-Entities.md) |
| `ability_info/` | Chỉ số, trigger, chi phí nâng cấp của ability | [JSON-Abilities](JSON-Abilities.md) |
| `classes/` | Lớp nhân vật | [JSON-Classes](JSON-Classes.md) |
| `quests/` | Quest | [JSON-Quests](JSON-Quests.md) |
| `npcs/` | NPC | [JSON-Npcs](JSON-Npcs.md) |
| `loottable/` | Loot table (số ít, không gạch dưới) | [JSON-Loot](JSON-Loot.md) |
| `crafting_recipes/` | Công thức chế tạo | [JSON-Crafting](JSON-Crafting.md) |
| `blocks/` | Block tùy chỉnh (generator, altar, ...) | [JSON-Blocks](JSON-Blocks.md) |
| `fuel_types/` | Loại nhiên liệu cho máy phát | [JSON-Blocks](JSON-Blocks.md#fuel_types--loại-nhiên-liệu) |
| `<thư mục trên>/tags/` | Tag — nhóm id lại | [Tag](#tag) bên dưới |
| `enchants/tags/` | Chỉ có tag (enchant là enum cứng, không có file entry) | [JSON-Items](JSON-Items.md#enchant--không-có-json-riêng) |
| `dungeon_v2/template/dungeon_template/` | Dungeon | [JSON-Dungeons](JSON-Dungeons.md) |
| `dungeon_v2/template/room_template/` | Phòng | [JSON-Dungeons](JSON-Dungeons.md) |
| `dungeon_v2/template/spawner_template/` | Spawner | [JSON-Dungeons](JSON-Dungeons.md) |
| `dungeon_v2/template/schemeta/`, `schematic/` | Metadata + file `.schem` của phòng (sinh bằng lệnh `/schemeta save`) | [JSON-Dungeons](JSON-Dungeons.md) |
| `global_config.json` | `mineskinApiKey`, `resourcePackUrl`, `resourcePackHash`, `lastDailyReset` | [Cấu hình chung](#cấu-hình-chung) |
| `schematics/island.schem` | Mẫu đảo (schematic WorldEdit) | — |

Do runtime tự sinh — **designer không sửa**: `dungeon_v2/runtime/**` (region, party, session), `island_grid_index.json`, các file `*.db` (SQLite: `player_data`, `player_quest_data`, `player_effect_data`, `island_data`, `blocks`), `generated/`. Plugin **không có** `config.yml`.

## Quy tắc chung cho các thư mục Registry

### Id của entry = đường dẫn file

Mỗi file `*.json` là 1 entry. Plugin đọc **đệ quy** mọi thư mục con; **id = đường dẫn tương đối từ thư mục gốc, bỏ `.json`**, dùng `/`:

```
items/fire_sword.json          -> id "fire_sword"
items/weapons/fire_sword.json  -> id "weapons/fire_sword"
```

Mọi chỗ khác tham chiếu entry (loot table gọi item, class liệt kê ability, block gọi drop...) đều dùng **id đầy đủ này**, gồm cả phần thư mục con.

<a id="field-id-trong-file-phải-khớp-id-đường-dẫn"></a>
### Field `"id"` trong file phải khớp id đường dẫn

Một số định nghĩa có thêm field `"id"` bên trong file (item, entity, quest, npc, class, crafting recipe). **Không có gì kiểm tra** field này khớp với đường dẫn, nhưng nhiều chỗ tra theo 1 trong 2 giá trị, nên lệch sẽ hỏng âm thầm (quest không lưu được tiến độ, entity không nạp lại sau khi chunk reload, NPC không tìm được định nghĩa, ...). **Luôn đặt `"id"` đúng bằng đường dẫn** (`entities/bosses/x.json` → `"id": "bosses/x"`). Loot table, block, ability config, skin, item type, fuel type **không có** field `"id"` — id chỉ đến từ đường dẫn.

### Thư mục `tags/` là chỗ dành riêng

Thư mục con tên `tags` trong mỗi thư mục dữ liệu không bao giờ được nạp như entry. Đừng đặt file entry trong đó.

### Lỗi không làm sập server

File sai → **log lỗi và bỏ qua đúng file đó**, các file khác vẫn nạp. Key lạ không có trong bảng → **bị bỏ qua im lặng** (gõ sai tên key = key đó "không tồn tại", không có cảnh báo). Hãy xem console sau khi load/reload.

### Cách đọc "bắt buộc / mặc định"

- **bắt buộc**: thiếu → fail cả file. Giá trị `null` tường minh được coi như thiếu.
- **optional, mặc định X**: thiếu → dùng X. Có mặt nhưng **sai kiểu** → vẫn **fail cả file** (không rơi về mặc định).
- Vài field "lenient" (ghi rõ trong từng trang): phần tử lỗi bị bỏ + log, phần còn lại vẫn nạp.

## Kiểu giá trị

| Kiểu | Quy tắc |
| :--- | :--- |
| string | Phải là chuỗi JSON thật. `5` không được chấp nhận cho field string. |
| int / float / double | Phải là số JSON, **không** chấp nhận chuỗi `"5"`. Số thập phân đưa vào field int bị cắt (`1.7` → `1`). |
| boolean | `true`/`false` thật. |
| enum | Chuỗi, **không phân biệt hoa/thường**, dấu gạch dưới giữ nguyên: `"max_health"`, `"MAX_HEALTH"` đều đúng. Phải khớp đúng tên hằng số (vd. `OFF_HAND`, không phải `OFFHAND`). Không có alias. |
| Material | Tên vật liệu (`"IRON_SWORD"`, hoặc `"minecraft:iron_sword"`). |
| Key | Chuỗi `"namespace:path"`; thiếu namespace = `minecraft:` (`"stone"` = `minecraft:stone`). Key sai ký tự làm fail cả file. |
| UUID | Chuỗi UUID chuẩn. |
| MiniMessage | Mọi chuỗi hiển thị cho người chơi (tên, lore, mô tả) dùng cú pháp MiniMessage: `<gold>`, `<red>`, `<gray>`, `<#ff8800>`, `<b>`... |
| Màu ARGB | Chuỗi `"A,R,G,B"`, 4 số 0–255 cách nhau dấu phẩy, vd. `"255,120,30,30"`. |
| Thời gian | **tick** (20 tick = 1 giây), trừ khi ghi rõ giây/phút. |

## Các "dạng" JSON hay gặp

**1. Giá trị trần** — 1 số/chuỗi/boolean/mảng đứng riêng, **không** có object bọc:

```json
"name": "<gold>Fire Sword",
"durability": 500,
"glint": true,
"description": ["dòng 1", "dòng 2"]
```

**2. Object có field** — các key cố định trong bảng.

**3. Object key tự do** — key do designer đặt/chọn từ 1 danh sách (vd. `attributes`: `{"MELEE_DAMAGE_BASE": 7.0}`).

**4. Object rỗng `{}`** — component không có tham số (`wrench`, `random_stat`).

**5. Đa hình (polymorphic)** — nhiều "loại" cùng 1 vị trí, loại được chọn bằng 1 field:

| Nơi dùng | Field chọn loại |
| :--- | :--- |
| Quest objective/requirement/reward, NPC action, Loot entry/condition, Crafting recipe, Ability upgrade requirement, Item ability, Spell của entity, Block, Generator behavior, Dungeon objective/room-event/behavior | **`"type"`** |
| Effect (trong `consumable.effects`) | **`"id"`** (khác hẳn!) |
| `components` của item/entity | **chính key của map** là loại component (không có field `type`) |

Giá trị `type`/`id` không tồn tại → fail cả file chứa nó (với danh sách thì **1 phần tử sai làm hỏng cả danh sách**, trừ chỗ ghi lenient).

## Tag

Mọi registry data-driven có thư mục `tags/`: `<thư mục>/tags/<tên>.json` là **mảng chuỗi trần** các id; entry bắt đầu bằng `#` là "gộp thêm tag khác" (đệ quy, vòng lặp tham chiếu bị phát hiện và log lỗi):

```json
// items/tags/swords.json
["iron_sword", "weapons/fire_sword", "#legendary_swords"]
```

- Id tag = **tên file** (không tính thư mục con), được hạ về chữ thường — viết tên tag chữ thường khi tham chiếu.
- Id trong tag không tồn tại → warning, bỏ qua riêng entry đó.
- Tag có sẵn (luôn tồn tại, rỗng nếu không có file): item `weapons`; entity `elite`, `boss`, `angelic`, `friendly`; quest `daily_easy_quest`, `daily_medium_quest`, `daily_hard_quest`.
- Tag được dùng ở: `item_types.tag`, `fuel_types.tag`, `classes.allowedWeapons`, quest hằng ngày, và các kiểm tra boss/elite/friendly của mob.

## Tham chiếu chéo sang registry khác

Field kiểu "id của entry khác" (`item_id` trong yêu cầu nâng cấp ability, `tools` của block, `type` của synergy module, ...) được nạp lười: **id gõ sai không làm fail file**, chỉ in cảnh báo `Unresolved reference '<id>' in registry '<thư mục>'` sau khi load xong, và tính năng đó âm thầm vô hiệu. Sau mỗi lần load, tìm dòng cảnh báo này trong log.

## Reload

Sửa JSON xong có thể nạp lại không cần restart bằng lệnh vận hành reload (`ReloadCommand`; toàn bộ hoặc từng registry). Dungeon có lệnh riêng `/template reload [all|spawner|dungeon|room|loottable|schemeta]`.

## Cấu hình chung

`global_config.json` được tạo mặc định nếu thiếu (Gson):

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `mineskinApiKey` | `"mineSkinApiKey"` | API key MineSkin cho lệnh `/skinfetch` |
| `resourcePackUrl` / `resourcePackHash` | — | Resource pack gửi cho người chơi |
| `lastDailyReset` | `0` | Mốc reset quest hằng ngày (plugin tự ghi) |

---
Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Items](JSON-Items.md)
