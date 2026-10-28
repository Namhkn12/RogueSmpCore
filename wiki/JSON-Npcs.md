# JSON Reference — NPCs

Thư mục: `npcs/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Spawn bằng lệnh `/smpnpc <id>` (id = đường dẫn file).

## Cấu trúc file

```json
{
  "id": "blacksmith_bob",
  "entityType": "MANNEQUIN",
  "name": "<gold>Bob",
  "skinId": "blacksmith_skin",
  "description": [
    "<gray>Thợ rèn của ngôi làng",
    "<yellow>Chuột phải để mở"
  ],
  "actions": [
    { "type": "run_command", "command": "tell @p Chào mừng!" },
    { "type": "open_gui", "gui": "blacksmith" }
  ]
}
```

NPC villager mặc đồ theo nghề:

```json
{
  "id": "librarian",
  "entityType": "VILLAGER",
  "name": "<aqua>Thủ thư",
  "profession": "minecraft:librarian",
  "actions": [
    { "type": "teleport", "destination": { "type": "warp", "warp": "library" } }
  ]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Id NPC. **Phải bằng đường dẫn file** (id được lưu vào NPC và tra lại theo đường dẫn). |
| `entityType` | enum `EntityType` | bắt buộc | Loại entity (vd. `MANNEQUIN`, `VILLAGER`). |
| `name` | string MiniMessage | optional | Tên hiển thị trên đầu (xem [Chữ trên đầu NPC](#chữ-trên-đầu-npc)). |
| `skinValue`, `skinSignature` | string | optional | Skin thô (Base64 + chữ ký). Chỉ có hiệu lực khi **cả 2** có mặt; nếu có thì **thắng** `skinId`. |
| `skinId` | string | optional | Id trong `skins/` (xem [JSON-Items](JSON-Items.md#skins--dữ-liệu-skin)). Id không tồn tại bị bỏ qua. |
| `description` | mảng string MiniMessage | `[]` | Mỗi phần tử 1 dòng mô tả, hiện dưới tên. Áp dụng cho **mọi** loại entity. Viết 1 string (dạng cũ) vẫn được, tính là 1 dòng. |
| `profession` | key nghề villager | optional | Nghề của villager, quyết định bộ đồ: `minecraft:armorer`, `butcher`, `cartographer`, `cleric`, `farmer`, `fisherman`, `fletcher`, `leatherworker`, `librarian`, `mason`, `nitwit`, `shepherd`, `toolsmith`, `weaponsmith`, `none`. Bỏ namespace thì mặc định `minecraft:`. Key sai → NPC không nạp được. **Chỉ Villager.** |
| `equipment` | object tên ô → đồ | `{}` | Trang bị NPC mặc/cầm: giáp nhuộm, giáp có trim, đầu người, vũ khí... Cùng định dạng với [`equipment` của entity](JSON-Entities.md#equipment). |
| `actions` | mảng [action](JSON-Actions.md) | **bắt buộc** (có thể `[]`) | Việc NPC làm khi bị click. |

⚠️ **Skin chỉ áp dụng khi `entityType` là `MANNEQUIN`, `profession` chỉ khi là `VILLAGER`.** Loại entity khác bỏ qua các key này.

### Trang bị

```json
"equipment": {
  "HEAD":  { "material": "PLAYER_HEAD", "headSkin": "knight_head" },
  "CHEST": { "material": "LEATHER_CHESTPLATE", "dyeColor": "#3B5BA5" },
  "LEGS":  { "material": "IRON_LEGGINGS", "trimMaterial": "minecraft:gold", "trimPattern": "minecraft:sentry" },
  "FEET":  { "material": "LEATHER_BOOTS", "dyeColor": "#222222", "trimMaterial": "minecraft:redstone", "trimPattern": "minecraft:rib" },
  "HAND":  { "material": "IRON_SWORD" }
}
```

- Định nghĩa là **nguồn duy nhất**: mỗi lần NPC được nạp, mọi ô bị xoá rồi mặc lại theo JSON. Bỏ 1 ô khỏi JSON rồi reload → ô đó trống.
- ⚠️ **Villager không hiện giáp** (model không có lớp giáp), chỉ hiện đồ cầm tay. Giáp hiện trên Mannequin và mob hình người (zombie, skeleton, piglin, armor stand...).
- Trình tạo NPC có nút `equipment` để chỉnh từng ô (dùng chung editor với trình tạo entity); trim được kiểm tra ngay khi lưu.

Hành vi NPC: bất tử, tắt AI, được lưu (persistent), Mannequin bị cố định; tự quay đầu nhìn người chơi gần nhất trong 5 block.

### Chữ trên đầu NPC

Tên và mô tả **không** dùng name tag của vanilla (name tag bị ẩn khi entity đang chở passenger). Thay vào đó NPC chở 2 text display:

- **Mô tả** — các dòng `description`, cách đỉnh đầu NPC 0.3 block. Không có mô tả thì không tạo.
- **Tên** — nằm ngay trên khối mô tả (cao thêm ~0.27 block mỗi dòng mô tả). Không có `name` thì không tạo.

Text display không được lưu vào world: mỗi lần NPC được nạp (spawn, chunk load, reload), các text display cũ bị xoá và tạo lại từ định nghĩa hiện tại, nên sửa JSON rồi reload là chữ cập nhật. Tên vẫn được đặt làm custom name của entity (ẩn) cho plugin khác đọc.

## Action

`actions` dùng định dạng action chung — xem [JSON-Actions](JSON-Actions.md) (loại action, id GUI, `click` trái/phải). Click phải vào NPC = `right`, đánh NPC = `left`; không ghi `click` thì action chạy với cả hai.

## Dành cho dev

Action dùng chung với item, code nằm ở package `action` — xem [JSON-Actions](JSON-Actions.md#dành-cho-dev).

Chữ trên đầu NPC và nghề villager nằm trong `BaseNpc.processEntity` ([`BaseNpc`](../src/main/java/com/roguesmp/npc/BaseNpc.java)); text display được tạo trễ 1 tick qua scheduler của entity. Trình tạo NPC trong game (`NpcCreatorGui`) chỉnh được mô tả nhiều dòng và chọn nghề từ danh sách.

---
◀ [JSON-Quests](JSON-Quests.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Actions](JSON-Actions.md)
