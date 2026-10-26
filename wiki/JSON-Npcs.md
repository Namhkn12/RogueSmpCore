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
| `actions` | mảng action | **bắt buộc** (có thể `[]`) | Việc NPC làm khi bị click. |

⚠️ **Skin chỉ áp dụng khi `entityType` là `MANNEQUIN`, `profession` chỉ khi là `VILLAGER`.** Loại entity khác bỏ qua các key này.

Hành vi NPC: bất tử, tắt AI, được lưu (persistent), Mannequin bị cố định; tự quay đầu nhìn người chơi gần nhất trong 5 block.

### Chữ trên đầu NPC

Tên và mô tả **không** dùng name tag của vanilla (name tag bị ẩn khi entity đang chở passenger). Thay vào đó NPC chở 2 text display:

- **Mô tả** — các dòng `description`, cách đỉnh đầu NPC 0.3 block. Không có mô tả thì không tạo.
- **Tên** — nằm ngay trên khối mô tả (cao thêm ~0.27 block mỗi dòng mô tả). Không có `name` thì không tạo.

Text display không được lưu vào world: mỗi lần NPC được nạp (spawn, chunk load, reload), các text display cũ bị xoá và tạo lại từ định nghĩa hiện tại, nên sửa JSON rồi reload là chữ cập nhật. Tên vẫn được đặt làm custom name của entity (ẩn) cho plugin khác đọc.

## Action

Field chọn loại: `"type"`. Mọi action trong mảng chạy **tuần tự** khi click (không phải chọn 1 trong nhiều).

| `type` | Key | Khi nào chạy | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `run_command` | `command` (string, bắt buộc) | click phải **và** trái | Người chơi chạy lệnh bằng quyền của chính họ (`performCommand`). **Viết không có `/` đầu.** `@p` được thay bằng tên người chơi. |
| `open_gui` | `gui` (string, bắt buộc) | chỉ click phải | Mở GUI theo id. |
| `open_trade` | `shop` (id trong `trades/`, bắt buộc) | chỉ click phải | Mở cửa hàng. Xem [JSON-Trades](JSON-Trades.md). |
| `teleport` | `destination` ([điểm đến](JSON-Warps.md#điểm-đến-destination), bắt buộc); `options` ([tuỳ chọn](JSON-Warps.md#tuỳ-chọn-dịch-chuyển-options), mặc định hiệu ứng `enderman`, chờ 30 tick) | chỉ click phải | Dịch chuyển người chơi. Trỏ tới warp bằng `{ "type": "warp", "warp": "<id>" }` để đổi điểm đến ở 1 chỗ. |

`gui` hợp lệ — chỉ 3 id, đăng ký cứng trong code: `blacksmith` (thợ rèn), `quest` ([bảng nhiệm vụ](JSON-Quests.md#bảng-nhiệm-vụ)), `inbox` (hộp thư thưởng). Id khác → người chơi nhận thông báo lỗi đỏ + log cảnh báo.

⚠️ Đừng dùng `@p_id`: `@p` được thay trước nên `@p_id` bị hỏng.

## Dành cho dev

Action đa hình: đăng ký trong [`NpcActions`](../src/main/java/com/roguesmp/npc/action/NpcActions.java). GUI mở được từ NPC: thêm `GuiOpenActions.OpenAction` mới trong [`GuiOpenActions`](../src/main/java/com/roguesmp/npc/action/GuiOpenActions.java). Lưu ý có 2 tầng dispatch khác nhau: `type` chọn loại action (`run_command`/`open_gui`/...), `gui` chọn GUI cụ thể trong 1 bảng tra riêng.

Chữ trên đầu NPC và nghề villager nằm trong `BaseNpc.processEntity` ([`BaseNpc`](../src/main/java/com/roguesmp/npc/BaseNpc.java)); text display được tạo trễ 1 tick qua scheduler của entity. Trình tạo NPC trong game (`NpcCreatorGui`) chỉnh được mô tả nhiều dòng và chọn nghề từ danh sách.

---
◀ [JSON-Quests](JSON-Quests.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Trades](JSON-Trades.md)
