# JSON Reference — NPCs

Thư mục: `npcs/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Spawn bằng lệnh `/smpnpc <id>` (id = đường dẫn file).

## Cấu trúc file

```json
{
  "id": "blacksmith_bob",
  "entityType": "MANNEQUIN",
  "name": "<gold>Bob",
  "skinId": "blacksmith_skin",
  "description": "<gray>Thợ rèn của ngôi làng",
  "actions": [
    { "type": "run_command", "command": "tell @p Chào mừng!" },
    { "type": "open_gui", "gui": "blacksmith" }
  ]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Id NPC. **Phải bằng đường dẫn file** (id được lưu vào NPC và tra lại theo đường dẫn). |
| `entityType` | enum `EntityType` | bắt buộc | Loại entity (vd. `MANNEQUIN`, `VILLAGER`). |
| `name` | string MiniMessage | optional | Tên hiển thị luôn hiện trên đầu. |
| `skinValue`, `skinSignature` | string | optional | Skin thô (Base64 + chữ ký). Chỉ có hiệu lực khi **cả 2** có mặt; nếu có thì **thắng** `skinId`. |
| `skinId` | string | optional | Id trong `skins/` (xem [JSON-Items](JSON-Items.md#skins--dữ-liệu-skin)). Id không tồn tại bị bỏ qua. |
| `description` | string MiniMessage | optional | Dòng mô tả dưới tên — **chỉ Mannequin**. |
| `actions` | mảng action | **bắt buộc** (có thể `[]`) | Việc NPC làm khi bị click. |

⚠️ **Skin và `description` chỉ áp dụng khi `entityType` là `MANNEQUIN`.** Loại entity khác bỏ qua mọi key skin và `description`.

Hành vi NPC: bất tử, tắt AI, được lưu (persistent), Mannequin bị cố định; tự quay đầu nhìn người chơi gần nhất trong 5 block.

## Action

Field chọn loại: `"type"`. Mọi action trong mảng chạy **tuần tự** khi click (không phải chọn 1 trong nhiều).

| `type` | Key | Khi nào chạy | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `run_command` | `command` (string, bắt buộc) | click phải **và** trái | Người chơi chạy lệnh bằng quyền của chính họ (`performCommand`). **Viết không có `/` đầu.** `@p` được thay bằng tên người chơi. |
| `open_gui` | `gui` (string, bắt buộc) | chỉ click phải | Mở GUI theo id. |

`gui` hợp lệ — chỉ 2 id, đăng ký cứng trong code: `blacksmith` (thợ rèn), `quest` (quest hằng ngày). Id khác → người chơi nhận thông báo lỗi đỏ + log cảnh báo.

⚠️ Đừng dùng `@p_id`: `@p` được thay trước nên `@p_id` bị hỏng.

## Dành cho dev

Action đa hình: đăng ký trong [`NpcActions`](../src/main/java/com/roguesmp/npc/action/NpcActions.java). GUI mở được từ NPC: thêm `GuiOpenActions.OpenAction` mới trong [`GuiOpenActions`](../src/main/java/com/roguesmp/npc/action/GuiOpenActions.java). Lưu ý có 2 tầng dispatch khác nhau: `type` chọn loại action (`run_command`/`open_gui`), `gui` chọn GUI cụ thể trong 1 bảng tra riêng.

---
◀ [JSON-Quests](JSON-Quests.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Loot](JSON-Loot.md)
