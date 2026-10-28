# JSON Reference — Action

Action là định dạng **dùng chung** cho "việc xảy ra khi người chơi click": dùng trong `actions` của [NPC](JSON-Npcs.md) và component [`actions`](JSON-Items.md#actions) của item. Không có thư mục riêng — action luôn nằm bên trong file khác. Quy tắc chung: [JSON-Overview](JSON-Overview.md).

## Một phần tử trong `actions`

Mỗi phần tử có 2 dạng, viết dạng nào cũng được:

```json
"actions": [
  { "type": "open_gui", "gui": "main_menu" },
  { "click": "left", "action": { "type": "run_command", "command": "spawn" } }
]
```

| Dạng | Khi nào dùng |
| :--- | :--- |
| Action trần `{ "type": ..., ... }` | Chạy với **mọi** kiểu click (`click` = `any`). |
| Bọc `{ "click": ..., "action": { ... } }` | Muốn giới hạn click trái hoặc phải. |

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `click` | `any` / `right` / `left` | `any` | Kiểu click kích hoạt action. Không phân biệt hoa thường. |
| `action` | action | bắt buộc (dạng bọc) | Action cần chạy — xem [Loại action](#loại-action). |

Mọi phần tử khớp kiểu click đều chạy **tuần tự** theo thứ tự trong mảng (không phải chọn 1 trong nhiều). Lưu lại từ trình tạo trong game luôn ghi dạng bọc.

## Loại action

Field chọn loại: `"type"`.

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `run_command` | `command` (string, bắt buộc) | Người chơi chạy lệnh bằng quyền của chính họ. **Viết không có `/` đầu.** `@p` → tên người chơi, `@p_id` → UUID. `command` rỗng thì không làm gì. |
| `open_gui` | `gui` (string, bắt buộc) | Mở GUI theo id — xem [Id GUI](#id-gui). |
| `teleport` | `destination` ([điểm đến](JSON-Warps.md#điểm-đến-destination), bắt buộc); `options` ([tuỳ chọn](JSON-Warps.md#tuỳ-chọn-dịch-chuyển-options), mặc định hiệu ứng `enderman`, chờ 30 tick, `safe: true`) | Dịch chuyển người chơi. Trỏ tới warp bằng `{ "type": "warp", "warp": "<id>" }` để đổi điểm đến ở 1 chỗ. |
| `entity_zapper` | — | Xoá entity bị click (kèm entity đang cưỡi lên nó). Click vào người chơi thì chỉ đẩy lùi + báo "NO!". Chỉ có tác dụng khi click **vào entity** — click không khí/block thì không làm gì. |

### Id GUI

Đăng ký cứng trong code:

| `gui` | Mở |
| :--- | :--- |
| `main_menu` | Menu chính |
| `warp` | Menu điểm dịch chuyển cho người chơi |
| `skill` | Bảng kĩ năng; người chơi chưa có lớp nhân vật thì mở chọn lớp kèm thông báo |
| `blacksmith` | Thợ rèn |
| `quest` | [Bảng nhiệm vụ](JSON-Quests.md#bảng-nhiệm-vụ) |
| `inbox` | Hộp thư thưởng |
| `shop:<shop_id>` | Cửa hàng, id trong `trades/` (xem [JSON-Trades](JSON-Trades.md)) |

Id khác → người chơi nhận thông báo lỗi đỏ + log cảnh báo.

## Click chạy ở đâu

| Nơi dùng | Click phải | Click trái |
| :--- | :--- | :--- |
| NPC | click phải vào NPC | đánh NPC |
| Item (`actions`) | click phải vào không khí, block, hoặc entity | click trái vào không khí, block, hoặc đánh entity |

Với item: sự kiện click chỉ bị huỷ khi có ít nhất 1 action **thực sự làm gì đó**. Ví dụ `entity_zapper` click vào không khí, `open_gui` với id sai, hay `run_command` rỗng không huỷ click — item vẫn dùng/đánh bình thường. Muốn item chỉ chạy action khi click phải mà vẫn đánh được bằng click trái, đặt `"click": "right"`.

⚠️ `entity_zapper` để `click` mặc định (`any`) sẽ xoá cả entity bị **đánh**. Thường nên đặt `"click": "right"`.

## Ví dụ

Item mở menu chính:

```json
"actions": {
  "hint": "<yellow>Chuột phải để mở menu",
  "actions": [ { "type": "open_gui", "gui": "main_menu" } ]
}
```

Cây xoá entity cho admin:

```json
"actions": {
  "hint": "<red><bold>Chuột phải để xóa entity khỏi world.",
  "actions": [ { "click": "right", "action": { "type": "entity_zapper" } } ]
}
```

NPC dịch chuyển khi click phải, chào khi bị đánh:

```json
"actions": [
  { "click": "right", "action": { "type": "teleport", "destination": { "type": "warp", "warp": "spawn" } } },
  { "click": "left", "action": { "type": "run_command", "command": "tell @p Đừng đánh tôi!" } }
]
```

## Chỉnh trong game

Trình tạo item (nút `actions`) và trình tạo NPC (nút `actions`) dùng chung 1 trình sửa: mỗi action là 1 ô JSON rút gọn theo đúng định dạng trên (không cần `{}` ngoài cùng, key không cần ngoặc kép).

## Dành cho dev

- Action là `PlayerAction` ([`PlayerAction`](../src/main/java/com/roguesmp/action/PlayerAction.java)), đăng ký codec trong [`PlayerActions`](../src/main/java/com/roguesmp/action/PlayerActions.java). `run(ActionContext)` trả `true` nếu action đã làm gì đó — dùng để quyết định có huỷ click không.
- [`ActionContext`](../src/main/java/com/roguesmp/action/ActionContext.java) chứa người chơi, kiểu click, item đang cầm (nếu có), entity bị click (nếu có), vị trí và event.
- [`ActionEntry`](../src/main/java/com/roguesmp/action/ActionEntry.java) = `click` + action; codec của nó đọc cả 2 dạng trần/bọc.
- GUI mới cho `open_gui`: thêm `OpenAction` trong [`GuiOpenActions`](../src/main/java/com/roguesmp/action/GuiOpenActions.java). Có 2 tầng dispatch khác nhau: `type` chọn loại action, `gui` chọn GUI trong 1 bảng tra riêng.

---
◀ [JSON-Npcs](JSON-Npcs.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Trades](JSON-Trades.md)
