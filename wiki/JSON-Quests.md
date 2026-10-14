# JSON Reference — Quests

Quest là nhiệm vụ có **mục tiêu** (objective) và **phần thưởng** (reward). Một quest có thể được giao cho:
- **1 người chơi** — quest hằng ngày, hoặc giao bằng lệnh.
- **1 nhóm** — mọi quest của 1 instance dungeon dùng chung tiến độ ([quest nhóm](#quest-nhóm-dungeon)).

Định nghĩa quest giống nhau cho cả hai; chỉ cách giao và cách tính tiến độ khác.

Thư mục: `quests/` (Registry, id = đường dẫn file: `quests/daily/gather_wood.json` → `daily/gather_wood`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Tiến độ được lưu trong database (`player_quest_data.db`, `group_quest_data.db`), không nằm trong JSON.

## File quest

```json
{
  "id": "slay_the_horde",
  "name": "<red>Tiêu diệt bầy quái",
  "icon": "IRON_SWORD",
  "description": ["Giết 20 zombie để bảo vệ ngôi làng."],
  "objectives": {
    "zombies": { "type": "kill_mob", "mobId": "zombie", "amount": 20 },
    "report": { "type": "signal", "signal": "talk_to_elder", "display": "Báo cáo trưởng làng" }
  },
  "rewards": [
    { "type": "item", "item": { "ruby_shard": 3 } },
    { "type": "money", "amount": 500 }
  ],
  "autoClaimRewards": false
}
```

| Key | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `id` | bắt buộc | Khoá lưu tiến độ. **Phải bằng đường dẫn file**, nếu không tag hằng ngày và tiến độ sẽ lệch nhau. |
| `name` | bắt buộc | Tên (MiniMessage) trong GUI, thông báo và `/quest info`. |
| `icon` | `PAPER` | Material làm icon trong GUI. |
| `description` | `[]` | Mô tả, mỗi phần tử 1 dòng. |
| `objectives` | `{}` | Map **tên → objective**. Tên do bạn đặt, dùng làm khoá lưu tiến độ (đổi tên = mất tiến độ của objective đó). Quest hoàn thành khi **mọi** objective xong; không có objective = hoàn thành ngay khi được giao. |
| `rewards` | bắt buộc (có thể `[]`) | Phần thưởng khi hoàn thành. |
| `autoClaimRewards` | `false` | Xem [nhận thưởng](#nhận-thưởng). Lưu ý viết **camelCase**. |
| `conditions` | `[]` | [Điều kiện](#condition) để hành động của người chơi được tính tiến độ. |
| `requirements` | `[]` | Được nạp nhưng **hiện chưa được kiểm tra ở đâu** — viết vào chưa chặn được gì. |

## Objective

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `kill_mob` | `{ "type": "kill_mob", "mobId": "zombie", "amount": 20 }` | +1 mỗi lần người chơi là người giết mob tuỳ chỉnh có id `mobId` (id trong `entities/`; mob vanilla **không** tính). Chỉ tính cho quest cá nhân. |
| `signal` | `{ "type": "signal", "signal": "gate_open", "amount": 3, "display": "Mở 3 cổng" }` | Tiến độ khi có **tín hiệu** cùng id được gửi tới (từ dungeon hoặc code). Tín hiệu mang số lượng; tổng không vượt `amount` (mặc định 1). `display` là nhãn trong GUI. |

`signal` là objective dùng chung cho mọi thứ không phải giết mob: dungeon gửi tín hiệu qua action `signal`, hệ thống khác (NPC, block...) có thể gửi bằng code.

## Condition

Hành động của người chơi (giết mob, tín hiệu từ người chơi) chỉ được tính khi **mọi** condition đúng tại thời điểm đó. Tiến độ do hệ thống gửi thẳng cho cả nhóm (action `signal` của dungeon, kill trong instance) không bị kiểm tra.

| `type` | Ví dụ | Đúng khi |
| :--- | :--- | :--- |
| `in_dungeon` | `{ "type": "in_dungeon" }`, `{ "type": "in_dungeon", "dungeon": "crypt" }` | Người chơi đang ở trong 1 instance dungeon mà họ là thành viên (tuỳ chọn: đúng dungeon `dungeon`). Với quest nhóm của dungeon, phải là **chính instance đó**. |
| `world` | `{ "type": "world", "world": "world_nether" }` | Người chơi đang ở world này. |
| `region` | `{ "type": "region", "region": "village" }` | Người chơi đang ở trong region WorldGuard này. |

## Reward

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `item` | `{ "type": "item", "item": { "fire_sword": 1, "ruby_shard": 5 } }` | Trao item tuỳ chỉnh (id trong `items/` → số lượng). Key là `item` số ít, giá trị là object. Id không tồn tại bị bỏ qua. |
| `money` | `{ "type": "money", "amount": 500 }` | Cộng tiền. |

## Requirement

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `date_single` | `{ "type": "date_single", "targetTimestamp": -1 }` | `-1` = luôn đúng. Khác `-1` = chỉ đúng **đúng 1 ngày**: giá trị phải là 0:00 sáng ngày đó theo giờ Việt Nam, tính bằng epoch millis. |

Chưa có chỗ nào kiểm tra requirement (xem trên).

## Nhận thưởng

Khi quest hoàn thành:
- `autoClaimRewards: true` và người chơi **đang online** → trao thưởng ngay.
- Ngược lại → phần thưởng vào **hộp thư thưởng** của người chơi, kèm thông báo có nút bấm nhận. Mở bằng `/quest rewards`.

Phần thưởng trong hộp thư **giữ 7 ngày**, quá hạn thì mất. Khi vào server, người chơi được nhắc nếu còn thưởng chưa nhận.

Quest nhóm: **mỗi người** trong danh sách nhận thưởng của nhóm nhận 1 phần riêng, theo cùng quy tắc trên.

## Quest hằng ngày

Có 3 độ khó, mỗi độ khó là 1 tag chứa danh sách quest có thể được chọn:

```json
// quests/tags/daily_easy_quest.json
["slay_the_horde", "daily/gather_wood"]
```

Các tag: `daily_easy_quest`, `daily_medium_quest`, `daily_hard_quest`. Tag thiếu hoặc rỗng → bỏ qua độ khó đó.

Hoạt động:
- Khi vào server và khi qua ngày mới (0:00 giờ Việt Nam), mỗi độ khó được bù cho đủ **3 quest đang làm**, chọn ngẫu nhiên từ tag, bỏ qua quest người chơi đang có.
- Mỗi độ khó tối đa **3 quest hoàn thành/ngày**. Nhận thưởng 1 quest hằng ngày → quest đó bị xoá và được thay bằng quest khác (nếu chưa đủ giới hạn).
- Qua ngày mới → xoá toàn bộ tiến độ quest hằng ngày và bộ đếm.

Quest **không** thuộc tag hằng ngày chỉ được giao bằng `/quest assign` hoặc từ dungeon.

## Quest nhóm (dungeon)

Feature `quests` trong [dungeon](JSON-Dungeons.md#quests--quest-nhóm) tạo 1 nhóm quest cho mỗi instance:

```json
"quests": { "quests": ["crypt_clear"], "eligible_on_enter": true }
```

- Các quest trong danh sách được giao khi instance bắt đầu. Action `start_quest` giao thêm quest trong lúc chơi.
- Tiến độ **dùng chung**: ai trong nhóm làm cũng tính, **ở bất kỳ đâu**. Muốn chỉ tính khi ở trong dungeon, thêm `"conditions": [ { "type": "in_dungeon" } ]` vào quest.
- Danh sách nhận thưởng: thành viên ban đầu, cộng người vào instance nếu `eligible_on_enter: true`. Chỉ người trong danh sách mới nhận thưởng.
- Tiến độ trong dungeon đến từ tín hiệu:
  - Action `signal` của dungeon → objective `signal` cùng id, cộng `amount`.
  - Giết mob trong instance → tín hiệu `kill:<id mob>`. Dùng `{ "type": "signal", "signal": "kill:hellknight" }` cho quest nhóm (objective `kill_mob` không tính cho nhóm).
- Quest nhóm hoàn thành → sự kiện dungeon `quest_completed` (subject = id quest), trigger bắt được.
- Instance kết thúc → nhóm quest kết thúc theo.

## Lệnh

| Lệnh | Tác dụng |
| :--- | :--- |
| `/quest info` | Xem quest đang làm và tiến độ. |
| `/quest rewards` | Mở hộp thư thưởng. |
| `/quest claim` | Nhận thưởng (dùng bởi nút bấm trong thông báo). |
| `/quest assign <player> <quest>` | Admin: giao quest cho người chơi. |

---
◀ [JSON-Classes](JSON-Classes.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Npcs](JSON-Npcs.md)
