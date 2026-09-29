# Quest System

Package: [`com.roguesmp.quest`](../src/main/java/com/roguesmp/quest)

Hệ quest độc lập hoàn toàn với [Dungeon System](Dungeon-System.md) (không có tích hợp nào giữa 2 hệ — phần thưởng dungeon đi qua [Loot System](Loot-System.md), không qua quest). 1 `Quest` là 1 template JSON: điều kiện để nhận (`requirements`), danh sách mục tiêu cần hoàn thành (`objectives`), và phần thưởng khi xong (`rewards`). Cả 3 nhóm đều đa hình, dispatch trên `"type"` qua [Codec System](Codec-System.md#polymorphic-dispatch-codecdispatch), mỗi nhóm có registry riêng trong [`Registries`](Registry-System.md): `QUEST_OBJECTIVE_CODEC`, `QUEST_REQUIREMENT_CODEC`, `QUEST_REWARD_CODEC`.

## `Quest` — định nghĩa

[`quest/Quest.java`](../src/main/java/com/roguesmp/quest/Quest.java), registry `Registries.QUEST = new Registry<>("quests", Quest.CODEC)`, thư mục `quests/`:

| JSON key | Kiểu | Bắt buộc/mặc định |
| :--- | :--- | :--- |
| `id` | String | bắt buộc |
| `name` | String | bắt buộc |
| `icon` | Material | optional, mặc định `PAPER` |
| `description` | mảng String | optional, mặc định `[]` |
| `requirements` | mảng `QuestRequirement` đa hình | optional, mặc định `[]` |
| `objectives` | `Map<String, QuestObjective>` đa hình — key là tên mục tiêu tùy đặt | optional, mặc định `{}` |
| `rewards` | mảng `QuestReward` đa hình | **bắt buộc** |

## `QuestObjective` — hiện chỉ có 1 kiểu

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `kill_mob` | `mobId` (String), `amount` (int) | Giết đủ `amount` con mob có id `mobId` |

Tiến độ runtime (`killed: int`, mặc định 0) của `kill_mob` được lưu qua 1 codec riêng, `OBJECTIVE_PROGRESS_CODEC` — populate như 1 tác dụng phụ của chính `QUEST_OBJECTIVE_CODEC.loadClass()` (mỗi loại objective tự đăng ký codec tiến độ của nó cùng lúc), không bootstrap độc lập.

## `QuestRequirement` — hiện chỉ có 1 kiểu

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `date_single` | `targetTimestamp` (long, epoch millis; `-1` = không giới hạn) | Quest chỉ nhận được trước/sau 1 mốc thời gian |

## `QuestReward` — 2 kiểu

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item` (`Map<String, Integer>` — id item → số lượng) | Trao 1 hoặc nhiều loại item |
| `money` | `amount` (long) | Trao tiền |

## Ví dụ đầy đủ — `quests/slay_the_horde.json`

```json
{
  "id": "slay_the_horde",
  "name": "Tiêu diệt bầy quái",
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

`"main"` trong `objectives` là tên tùy đặt (không phải id có ý nghĩa đặc biệt) — 1 quest có thể có nhiều mục tiêu song song, mỗi cái 1 key riêng trong map.

## Tag quest có sẵn — quest hằng ngày

[`Tags.java`](../src/main/java/com/roguesmp/tag/Tags.java) khai sẵn 3 tag built-in trên `Registries.QUEST`: `daily_easy_quest`, `daily_medium_quest`, `daily_hard_quest`. Thêm 1 quest vào vòng xoay "quest hằng ngày" theo độ khó chỉ đơn giản là thêm id của nó vào file tag tương ứng:

```json
// quests/tags/daily_easy_quest.json
["slay_the_horde", "gather_wood"]
```

Xem [Registry System → Tag](Registry-System.md#tag-system) để biết cơ chế tag đầy đủ (bao gồm cách 1 tag có thể `#tham-chiếu` tag khác).

## Cách thêm 1 loại objective/requirement/reward mới

1. Tạo class implement `QuestObjective`/`QuestRequirement`/`QuestReward` tương ứng, khai `public static final Codec<YourType> CODEC` với 1 field trả `getTypeId()`.
2. Đăng ký vào registry tương ứng trong `QuestObjectives.java`/`QuestRequirements.java`/`QuestRewards.java` (mỗi file có 1 `loadClass()` no-op để ép static initializer chạy, cùng pattern mọi registry đa hình khác trong dự án).
3. Dùng `"type": "your_id"` trong file `quests/*.json`.

## Lưu ý & lỗi thường gặp

- **`rewards` là field bắt buộc duy nhất trong `Quest.CODEC`** — 1 quest thiếu `rewards` fail decode hoàn toàn, còn thiếu `objectives`/`requirements` chỉ rơi về rỗng.
- **Dungeon và Quest không tích hợp với nhau** — đừng tìm cách 1 phòng dungeon hoàn thành sẽ tự cấp quest reward hay ngược lại; nếu cần luồng đó, phải tự viết code nối 2 hệ, hiện chưa có sẵn.

---
◀ [Dungeon System](Dungeon-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [NPC System](Npc-System.md)
