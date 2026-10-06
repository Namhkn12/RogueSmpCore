# JSON Reference — Quests

Thư mục: `quests/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Tiến độ của người chơi **không** nằm trong JSON (lưu ở `player_quest_data.db`).

## Cấu trúc file

```json
{
  "id": "slay_the_horde",
  "name": "<red>Tiêu diệt bầy quái",
  "icon": "IRON_SWORD",
  "description": ["Giết 20 zombie để giúp ngôi làng."],
  "requirements": [ { "type": "date_single", "targetTimestamp": -1 } ],
  "objectives": {
    "main": { "type": "kill_mob", "mobId": "zombie", "amount": 20 }
  },
  "rewards": [
    { "type": "item", "item": { "ruby_shard": 3 } },
    { "type": "money", "amount": 500 }
  ]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Khóa lưu tiến độ. **Phải bằng đường dẫn file** — tag quest hằng ngày tra theo đường dẫn, còn tiến độ lưu theo `id`; lệch là hỏng liên kết. |
| `name` | string MiniMessage | bắt buộc | Tên trong GUI và `/quest info`. |
| `icon` | Material | optional, `PAPER` | Icon trong GUI. |
| `description` | mảng string MiniMessage | optional, `[]` | Dòng lore trong GUI. |
| `requirements` | mảng requirement | optional, `[]` | ⚠️ **Hiện không được áp dụng ở đâu cả** (xem dưới). |
| `objectives` | object: **tên mục tiêu → objective** | optional, `{}` | Các mục tiêu. Tên mục tiêu tự đặt (khóa lưu tiến độ, hiện trong `/quest info`); thứ tự không đảm bảo. |
| `rewards` | mảng reward | **bắt buộc** (có thể `[]`) | Nhận khi hoàn thành. |

Quest **không có `objectives`** được coi là hoàn thành ngay khi được giao (hoàn thành = không còn objective chưa xong).

## Objective

Hiện có **1 loại**. Field chọn loại: `"type"`.

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `kill_mob` | `mobId` (string, bắt buộc), `amount` (int, bắt buộc) | +1 mỗi lần người chơi là người gây ra cái chết của mob có id `mobId` (**id trong `entities/`**). |

Chỉ có sự kiện "giết mob" được nối vào hệ quest.

## Requirement

Hiện có **1 loại**:

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `date_single` | `targetTimestamp` (long, bắt buộc) | `-1` = luôn đúng. Khác `-1` = chỉ đúng **vào đúng 1 ngày**: giá trị phải bằng **0:00 sáng ngày đó theo giờ Việt Nam (GMT+7), tính bằng epoch millis**. (Đây là phép so sánh bằng, không phải "trước/sau" 1 mốc; số không đúng nửa đêm VN sẽ không bao giờ khớp.) |

⚠️ Mảng `requirements` được decode nhưng **hiện chưa có chỗ nào kiểm tra** (không có luồng "nhận quest" gọi nó): viết requirement chưa chặn được gì.

## Reward

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item` (object: **id item → số lượng**, bắt buộc) | Trao từng item. Key số ít `item`, giá trị là **object**, không phải mảng. |
| `money` | `amount` (long, bắt buộc) | Trao tiền. |

`item` chỉ nhận **id item tùy chỉnh** (trong `items/`), **không** nhận `minecraft:...`; id không tồn tại bị bỏ qua im lặng khi trao.
```json
{ "type": "item", "item": { "fire_sword": 1, "ruby_shard": 5 } }
```

## Quest hằng ngày & tag

3 tag built-in trên quest: `daily_easy_quest`, `daily_medium_quest`, `daily_hard_quest`. File tag nằm ở `quests/tags/<tên>.json`, là mảng id quest (đường dẫn file) hoặc `#tag`:

```json
// quests/tags/daily_easy_quest.json
["slay_the_horde", "daily/gather_wood"]
```

Hoạt động:
- Tier có tag rỗng/thiếu file → bị bỏ qua.
- Khi vào server và khi reset ngày, mỗi tier được bù cho đủ quest đang hoạt động (tối đa **3 quest/tier**, giới hạn 3 quest hoàn thành/tier/ngày — cố định trong code), chọn ngẫu nhiên từ tag, bỏ qua quest người chơi đang có tiến độ.
- Reset khi `lastDailyCompletionResetTimestamp` cũ hơn **0:00 hôm nay giờ Việt Nam**: xóa toàn bộ tiến độ quest hằng ngày + đếm hoàn thành.
- Nhận thưởng qua GUI hằng ngày sẽ trao thưởng, tăng bộ đếm tier, xóa quest đó và giao quest thay thế (khác quest vừa xong) khi tier chưa đủ 3.
- Quest **không** thuộc tag hằng ngày **không có luồng nhận**: chỉ được giao bằng lệnh `/quest assign <player> <quest_id>`, và nhận thưởng qua `QuestGui`.

## Dành cho dev

Objective/requirement/reward là đa hình dispatch trên `"type"`: tạo class + `CODEC`, đăng ký trong `QuestObjectives` / `QuestRequirements` / `QuestRewards` (mỗi file có `loadClass()` ép static init chạy). `OBJECTIVE_PROGRESS_CODEC` (tiến độ runtime của từng loại objective) được đăng ký cùng lúc ở objective. Quest hoàn toàn độc lập với [dungeon](JSON-Dungeons.md) — không có tích hợp. `DailyObjective` là class rỗng chưa đăng ký.

---
◀ [JSON-Classes](JSON-Classes.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Npcs](JSON-Npcs.md)
