# NPC System

Package: [`com.roguesmp.npc`](../src/main/java/com/roguesmp/npc)

1 `BaseNpc` là định nghĩa JSON cho 1 NPC đứng yên (thường là entity `PLAYER` mang skin tùy chỉnh) với 1 danh sách hành động khi player tương tác (`actions`), đa hình trên `"type"` qua [`Registries.NPC_ACTION_CODEC`](Registry-System.md).

## `BaseNpc` — định nghĩa

[`npc/BaseNpc.java`](../src/main/java/com/roguesmp/npc/BaseNpc.java), registry `Registries.NPC = new Registry<>("npcs", BaseNpc.CODEC)`, thư mục `npcs/`:

| JSON key | Kiểu | Bắt buộc/mặc định |
| :--- | :--- | :--- |
| `id` | String | bắt buộc |
| `entityType` | `org.bukkit.entity.EntityType` | bắt buộc |
| `name` | String | optional, có thể `null` |
| `skinValue` / `skinSignature` | String | optional |
| `skinId` | String | optional — id tra trong `SkinRegistry`, thường dùng thay vì khai `skinValue`/`skinSignature` tay |
| `description` | String | optional |
| `actions` | mảng `NpcAction` đa hình | **bắt buộc** |

## `NpcAction` — 2 kiểu đã đăng ký

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `run_command` | `command` (String) | Chạy 1 lệnh console, hỗ trợ placeholder `@p` (tên player)/`@p_id` (UUID) trong chuỗi lệnh |
| `open_gui` | `gui` (String) | Mở 1 GUI theo id — id này được tra ở **1 registry riêng**, [`Registries.NPC_GUI_OPEN_ACTION`](../src/main/java/com/roguesmp/npc/action/GuiOpenActions.java), hiện có đúng 2 id hardcode: `"blacksmith"`, `"quest"` |

```json
{
  "id": "blacksmith_bob",
  "entityType": "PLAYER",
  "name": "Bob",
  "skinId": "blacksmith_skin",
  "description": "Thợ rèn của ngôi làng",
  "actions": [
    { "type": "run_command", "command": "tell @p Chào mừng!" },
    { "type": "open_gui", "gui": "blacksmith" }
  ]
}
```

`actions` là 1 mảng — mọi action trong đó chạy tuần tự khi player tương tác, không phải chọn 1 trong nhiều.

## Thêm 1 GUI mới mở được từ NPC

`open_gui` chỉ tra được những id đã đăng ký cứng trong code, [`GuiOpenActions.java`](../src/main/java/com/roguesmp/npc/action/GuiOpenActions.java) — viết 1 JSON với `"gui": "some_new_id"` chưa đăng ký sẽ không mở được gì (không lỗi rõ ràng, chỉ đơn giản là không có hành động nào xảy ra). Thêm 1 id mới: đăng ký 1 `GuiOpenActions.OpenAction` mới trong file đó, trỏ tới constructor GUI mong muốn.

## Cách thêm 1 loại `NpcAction` mới

1. Tạo class implement `NpcAction`, khai `public static final Codec<YourAction> CODEC`.
2. Đăng ký vào [`NpcActions.java`](../src/main/java/com/roguesmp/npc/action/NpcActions.java) (`loadClass()` no-op để ép static init chạy, cùng pattern mọi registry đa hình khác).
3. Dùng `"type": "your_id"` trong `actions` của 1 file `npcs/*.json`.

## Lưu ý & lỗi thường gặp

- **`open_gui`'s `gui` id không cùng registry với `NpcAction`'s `type`** — 2 tầng dispatch khác nhau, đừng nhầm lẫn: `type` chọn *loại action* (`run_command`/`open_gui`), còn `gui` (chỉ tồn tại khi `type` là `open_gui`) chọn *GUI cụ thể nào* trong 1 bảng tra riêng, nhỏ hơn nhiều và hoàn toàn code-driven.
- **`skinId` ưu tiên hơn khai `skinValue`/`skinSignature` tay** — dùng `skinId` trỏ vào `SkinRegistry` trừ khi có lý do cụ thể cần giá trị texture Mojang thô.

---
◀ [Quest System](Quest-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Loot System](Loot-System.md)
