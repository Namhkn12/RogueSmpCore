# JSON Reference — Quests

Quest là nhiệm vụ có **mục tiêu** (objective) và **phần thưởng** (reward). Một quest có thể được giao cho:
- **1 người chơi** — nhận từ [bảng nhiệm vụ](#bảng-nhiệm-vụ), hoặc giao bằng lệnh.
- **1 nhóm** — mọi quest của 1 instance dungeon dùng chung tiến độ ([quest nhóm](#quest-nhóm-dungeon)).

Định nghĩa quest giống nhau cho cả hai; chỉ cách giao và cách tính tiến độ khác.

Thư mục: `quests/` (Registry, id = đường dẫn file: `quests/board/gather_wood.json` → `board/gather_wood`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Tiến độ được lưu trong database (`player_quest_data.db`, `group_quest_data.db`), không nằm trong JSON.

## File quest

```json
{
  "id": "slay_the_horde",
  "name": "<red>Tiêu diệt bầy quái",
  "icon": "minecraft:iron_sword",
  "description": ["Giết zombie để bảo vệ ngôi làng."],
  "objectives": {
    "zombies": { "type": "kill_mob", "mobId": "zombie", "amount": { "min": 15, "max": 25 } },
    "report": { "type": "signal", "signal": "talk_to_elder", "display": "Báo cáo trưởng làng" }
  },
  "rewards": [
    { "type": "item", "item": { "ruby_shard": 3 } },
    { "type": "money", "amount": 500 },
    { "type": "quest_exp", "amount": 50 }
  ],
  "board": { "reset": "P1D", "weight": 2 },
  "autoClaimRewards": false
}
```

| Key | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `id` | bắt buộc | Khoá lưu tiến độ. **Phải bằng đường dẫn file**, nếu không tiến độ sẽ bị lệch. |
| `name` | bắt buộc | Tên (MiniMessage) trong GUI, thông báo và `/quest info`. |
| `icon` | `minecraft:paper` | **Item model** làm icon trong GUI và hộp thư (vd. `minecraft:iron_sword`, hoặc model trong resource pack). |
| `description` | `[]` | Mô tả, mỗi phần tử 1 dòng. |
| `objectives` | `{}` | Map **tên → objective**. Tên do bạn đặt, dùng làm khoá lưu tiến độ (đổi tên = mất tiến độ của objective đó). Quest hoàn thành khi **mọi** objective xong; không có objective = hoàn thành ngay khi được giao. |
| `rewards` | bắt buộc (có thể `[]`) | Phần thưởng khi hoàn thành. |
| `autoClaimRewards` | `false` | Xem [nhận thưởng](#nhận-thưởng). Lưu ý viết **camelCase**. |
| `conditions` | `[]` | [Điều kiện](#condition) để hành động của người chơi được tính tiến độ. |
| `requirements` | `[]` | [Requirement](#requirement) để quest lên bảng và được nhận. |
| `board` | không có | Có key này → quest nằm trong [bảng nhiệm vụ](#bảng-nhiệm-vụ). |

## Objective

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `kill_mob` | `{ "type": "kill_mob", "mobId": "zombie", "amount": 20 }` | +1 mỗi lần người chơi là người giết mob tuỳ chỉnh có id `mobId` (id trong `entities/`; mob vanilla **không** tính). `amount` **bắt buộc**. Chỉ tính cho quest cá nhân. |
| `signal` | `{ "type": "signal", "signal": "gate_open", "amount": 3, "display": "Mở 3 cổng" }` | Tiến độ khi có **tín hiệu** cùng id được gửi tới (từ dungeon hoặc code). Tín hiệu mang số lượng; tổng không vượt `amount` (mặc định 1). `display` là nhãn trong GUI. |
| `submit_item` | `{ "type": "submit_item", "item": "ruby_shard", "amount": 10 }` | Nộp item tuỳ chỉnh (id trong `items/`). Người chơi **chuột trái** vào quest trong bảng nhiệm vụ để nộp. Phải có **đủ** số lượng mới nộp được; nộp 1 lần, lấy đúng `amount` (mặc định 1). |

`signal` là objective dùng chung cho mọi thứ không phải giết mob hay nộp đồ: dungeon gửi tín hiệu qua action `signal`, hệ thống khác (NPC, block...) có thể gửi bằng code.

### `amount` dạng khoảng

Mọi `amount` của objective có thể là 1 số, hoặc khoảng `{ "min": 10, "max": 20 }`. Với khoảng, số cần đạt được chọn ngẫu nhiên khi quest được giao. Với quest bảng, kết quả cố định trong 1 chu kỳ: mọi người chơi nhận quest đó trong cùng chu kỳ có cùng số cần đạt.

## Condition

Hành động của người chơi (giết mob, tín hiệu từ người chơi, nộp đồ) chỉ được tính khi **mọi** condition đúng tại thời điểm đó. Tiến độ do hệ thống gửi thẳng cho cả nhóm (action `signal` của dungeon, kill trong instance) không bị kiểm tra.

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
| `quest_exp` | `{ "type": "quest_exp", "amount": 50 }` | Cộng kinh nghiệm Mạo hiểm (xem [Cấp Mạo hiểm](#cấp-mạo-hiểm)). |

## Requirement

Dùng chung loại requirement với cửa hàng NPC: xem [JSON-Trades › Requirement](JSON-Trades.md#requirement-requirements) để biết các `type` (`level`, `quest_completed`, `permission`, `money`, `item`, `date`).

Với quest bảng, requirement được kiểm tra 2 lần:
- Khi bảng được làm đầy: quest không thoả sẽ không được chọn lên bảng của người chơi đó.
- Khi người chơi bấm nhận: không thoả thì báo lỗi và không nhận.

Quest giao bằng `/quest assign` hoặc từ dungeon **không** kiểm tra requirement.

## Nhận thưởng

Khi quest hoàn thành, người chơi nhận thông báo trong chat, rồi:
- `autoClaimRewards: true` và người chơi **đang online** → trao thưởng ngay.
- Ngược lại → phần thưởng vào **hộp thư thưởng** của người chơi. Mở bằng `/quest inbox` hoặc nút hộp thư trong GUI.

Phần thưởng trong hộp thư **giữ 7 ngày**, quá hạn thì mất. Khi vào server, người chơi được nhắc nếu còn thưởng chưa nhận, kèm nút **[Mở hộp thư]**.

Quest nhóm: **mỗi người** trong danh sách nhận thưởng của nhóm nhận 1 phần riêng, theo cùng quy tắc trên.

## Bảng nhiệm vụ

Quest có key `board` sẽ nằm trong bảng nhiệm vụ (`/quest board`):

```json
"board": { "reset": "P1D", "weight": 2 }
```

| Key | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `reset` | bắt buộc | Chu kỳ làm mới, dạng ISO-8601: `P1D` = hằng ngày, `P2D` = 2 ngày, `P7D` = hằng tuần, `PT12H` = 12 giờ. Mọi chu kỳ tính từ thứ Hai 05/01/2026 0:00 giờ Việt Nam, nên mọi quest cùng chu kỳ làm mới **cùng lúc**. |
| `weight` | `1` | Tỉ lệ được chọn lên bảng so với quest khác. |

Hoạt động:
- Mỗi người chơi có bảng riêng gồm tối đa `size` quest (cấu hình trong `quest_board.json`, xem [Cấp Mạo hiểm](#cấp-mạo-hiểm)), chọn ngẫu nhiên theo `weight` từ các quest thoả requirement.
- Người chơi **bấm** để nhận quest. Số quest đang làm cùng lúc bị giới hạn theo Cấp Mạo hiểm.
- **Shift + chuột phải** vào quest đang làm để huỷ: mất tiến độ, quest vẫn ở trên bảng để nhận lại.
- Quest hoàn thành → rời khỏi bảng, không xuất hiện lại cho đến chu kỳ sau. Chỗ trống được bù ngay bằng quest khác.
- Hết chu kỳ của quest → quest bị gỡ khỏi bảng, **tiến độ chưa xong bị xoá**. Bảng được kiểm tra mỗi phút và mỗi khi mở GUI.

## Cấp Mạo hiểm

Cấu hình trong `quest_board.json` (thư mục plugin). Thiếu file hoặc file lỗi → dùng giá trị mặc định.

```json
{
  "size": 7,
  "level": {
    "baseExp": 100,
    "increment": 50,
    "maxLevel": 50,
    "acceptLimits": { "1": 2, "5": 3, "10": 4, "20": 5 },
    "rewards": {
      "5":  [ { "type": "money", "amount": 1000 } ],
      "10": [ { "type": "money", "amount": 2500 }, { "type": "item", "item": { "ruby_shard": 5 } } ]
    }
  }
}
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `size` | `7` | Số quest hiện trên bảng nhiệm vụ (1–28). |
| `level.baseExp` | `100` | Exp cần để từ cấp 1 lên cấp 2. |
| `level.increment` | `50` | Mỗi cấp sau cần thêm bấy nhiêu exp: cấp `n` → `n+1` cần `baseExp + increment × (n − 1)`. |
| `level.maxLevel` | `50` | Cấp tối đa. |
| `level.acceptLimits` | `{1:2, 5:3, 10:4, 20:5}` | Cấp → số quest trên bảng được nhận cùng lúc, áp dụng từ cấp đó trở lên. |
| `level.rewards` | `{}` | Cấp → danh sách [reward](#reward), trao khi người chơi đạt cấp đó. |

Phần thưởng cấp:
- Luôn được gửi vào **hộp thư thưởng** (nguồn "Cấp Mạo hiểm"), giữ 7 ngày như các phần thưởng khác.
- Lên nhiều cấp một lúc → nhận đủ phần thưởng của **mọi cấp** đã vượt qua.
- Không bao giờ nhận lại, vì exp chỉ tăng chứ không giảm.

Trong bảng nhiệm vụ, bấm nút **Cấp Mạo hiểm** để xem phần thưởng từng cấp. Các cấp được xếp theo đường zíc zắc, 29 cấp mỗi trang. Cấp có phần thưởng hiện là rương. Cấp không có thưởng hiện là kính màu: xanh lá = đã đạt, vàng = cấp kế tiếp, đỏ = chưa đạt.

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
| `/quest` | Mở GUI danh sách quest đang làm / đã xong. |
| `/quest board` | Mở bảng nhiệm vụ. |
| `/quest inbox` | Mở hộp thư thưởng. |
| `/quest info <quest>` | Xem định nghĩa 1 quest (mục tiêu, phần thưởng). |
| `/quest assign <player> <quest>` | Admin: giao quest cho người chơi. Quyền `quest.assign`. |

---
◀ [JSON-Classes](JSON-Classes.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Npcs](JSON-Npcs.md)
