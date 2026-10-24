# JSON Reference — Dungeons

Một dungeon là **1 world riêng cho mỗi nhóm chơi** (gọi là *instance*), sao chép từ 1 Polar template. Bản thân lõi dungeon chỉ biết tạo world, quản lý thành viên và lưu dữ liệu; **mọi luật chơi** (hết hạn, phòng ngẫu nhiên, hồi sinh, rương thưởng, quest, script...) là các **feature** bạn bật trong JSON. Một dungeon không có feature nào vẫn chạy được — chỉ là 1 world trống để đi dạo.

Quy tắc chung (id = đường dẫn file, kiểu giá trị, lỗi khi nạp): [JSON-Overview](JSON-Overview.md). Kiến trúc cho dev: [Dungeon System](Dungeon-System.md).

## Thư mục

| Đường dẫn (trong `plugins/RogueSmpCore/`) | Chứa gì |
| :--- | :--- |
| `dungeon/definitions/` | Định nghĩa dungeon. `dungeon/definitions/crypt.json` → id `crypt`. |
| `dungeon/rooms/` | Định nghĩa phòng, dùng bởi feature `rooms` và `treasure`. |
| `dungeon/schematics/` | File `.schem` của phòng. Tạo bằng `/dungeonbuild schematic save <tên>`. |
| `dungeon/instances/` | World của các instance đang chơi — **runtime, không sửa tay**. |
| `dungeon/instances.db` | Dữ liệu instance đang chơi (thành viên, biến, state feature) — **runtime**. |

Định nghĩa dungeon và phòng nạp lại được bằng `/smpreload` mà không cần restart. Nên thử thay đổi trên instance mới tạo.

## Vòng đời 1 instance

1. Người chơi mở `/dungeonrun`, chọn dungeon, bấm **Tạo phó bản**. Mỗi người chỉ có **1 instance cho mỗi dungeon** cùng lúc.
2. Plugin sao chép world template, chạy các feature (vd. `rooms` chọn trước danh sách phòng, `expiry` đặt hạn), rồi bắn sự kiện `started` và đưa người chơi vào.
3. Trong lúc chơi, mọi việc xảy ra (vào/rời, giết mob, nhặt đồ, chết...) được đổi thành **sự kiện** và gửi tới từng feature.
4. Người chơi có thể thoát ra và **vào lại** sau từ menu — instance vẫn giữ nguyên (world được unload khoảng 1 phút sau khi không còn ai bên trong, nạp lại khi có người vào).
5. Instance kết thúc khi: hết hạn (`expiry`), cả đội gục (`revive`), người cuối cùng rời bỏ, action `end`, hoặc admin `/dungeonrun admin end`. World và dữ liệu bị xoá.

Mọi thành viên có **quyền ngang nhau** — không có "chủ phó bản". Ai cũng mời được (nếu dungeon cho phép) và ai rời đi cũng không làm hỏng instance của người khác.

## File dungeon

```json
{
  "template": "crypt",
  "display_name": "<gold><b>Hầm mộ",
  "icon": "minecraft:skeleton_skull",
  "lore": ["<gray>Một hầm mộ cổ đầy xương.", "<gray>Thu thập 3 chìa khoá để mở ngai vàng."],
  "variables": { "keys": 0, "gate_open": false },
  "features": {
    "expiry": { "duration": "2h" },
    "membership": {},
    "triggers": { "triggers": [] }
  }
}
```

| Key | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `template` | bắt buộc | Tên Polar template world. Mỗi instance là 1 bản sao của world này. |
| `display_name` | bắt buộc | Tên hiển thị (MiniMessage) trong menu, tin nhắn, scoreboard. |
| `icon` | `minecraft:paper` | Item model hiển thị trong menu `/dungeonrun`. |
| `lore` | `[]` | Mô tả trong menu, mỗi phần tử 1 dòng. |
| `variables` | `{}` | Giá trị ban đầu của [biến](#biến) cho mỗi instance mới. |
| `features` | `{}` | Map **key → feature**. |

**Key và `type` của feature.** Nếu không ghi `"type"`, type lấy bằng chính key — nên `"expiry": { "duration": "2h" }` là đủ. Chỉ ghi `"type"` khi muốn 2 feature cùng loại, vì key phải khác nhau:

```json
"triggers": { "triggers": [ ... ] },
"boss_script": { "type": "triggers", "triggers": [ ... ] }
```

## Feature

### `expiry` — thời hạn

```json
"expiry": { "duration": "2h" }
```

Instance tự kết thúc khi hết hạn, kể cả khi không ai online. `duration` là số + đơn vị: `m` (phút), `h` (giờ), `d` (ngày), `w` (tuần); mặc định `1w`. Thời gian còn lại hiện trong menu, scoreboard và condition `time_left`. Feature `treasure` có thể đặt lại hạn sau khi hạ boss.

### `membership` — mời và rời bỏ

```json
"membership": { "invites": true, "abandon": true }
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `invites` | `true` | Thành viên được mời người khác. Lời mời hết hạn sau 60 giây. |
| `abandon` | `true` | Thành viên được rời bỏ instance vĩnh viễn (mất quyền vào lại trừ khi được mời lại). Người cuối cùng rời → instance bị huỷ. |

**Không có feature này = không mời và không rời bỏ được.** Dungeon chơi 1 lần cho cả nhóm thường bỏ `membership` và dùng `party`.

### `party` — vào cùng nhóm

```json
"party": {}
```

Khi trưởng nhóm (`/party`) tạo instance, **cả nhóm** được thêm làm thành viên và dịch chuyển vào cùng lúc — không cần mời từng người. Không có nhóm thì người tạo vào một mình. Thành viên nhóm không phải trưởng nhóm không tạo được instance cho cả nhóm. Mỗi thành viên vẫn phải chưa có instance của dungeon này, nếu không việc tạo bị từ chối.

### `scoreboard` — bảng thông tin

```json
"scoreboard": { "priority": 30 }
```

Hiện scoreboard TAB khi ở trong instance: tên dungeon, thời gian còn lại (nếu có `expiry`), phòng hiện tại + tiến độ từng objective + điểm (nếu có `rooms`). `priority` quyết định scoreboard nào thắng khi nhiều hệ cùng muốn hiện.

### `quests` — quest nhóm

```json
"quests": { "quests": ["crypt_clear"], "eligible_on_enter": true }
```

Mỗi instance có 1 nhóm quest riêng; các quest trong `quests` được giao khi instance bắt đầu. Tiến độ **dùng chung** cho cả nhóm. Khi hoàn thành, mỗi người trong danh sách nhận thưởng sẽ nhận phần thưởng riêng. `eligible_on_enter: true` = ai vào instance là được thêm vào danh sách đó. Chi tiết: [JSON-Quests](JSON-Quests.md#quest-nhóm-dungeon).

### `triggers` — script

```json
"triggers": { "triggers": [ { "on": "started", "actions": [ ... ] } ] }
```

Danh sách [trigger](#trigger). Đây là cách chính để dựng cốt truyện, câu đố và sự kiện riêng của dungeon mà không cần code.

### `rooms` — chuỗi phòng ngẫu nhiên

```json
"rooms": {
  "minimum_rooms": 4,
  "max_choices": 3,
  "pools": [
    { "min": 2, "max": 3, "rooms": [ { "room": "crypt_hall", "weight": 3 }, { "room": "crypt_puzzle" } ] },
    { "rooms": [ { "room": "crypt_boss" } ] }
  ]
}
```

Cách hoạt động:

1. **Khi instance bắt đầu**, mỗi pool rút ngẫu nhiên từ `min` đến `max` phòng (theo `weight`, không trùng). Gộp lại thành danh sách phòng của lượt chơi này.
2. Template world có **cửa là block vault**. Người chơi tương tác vault → menu hiện **1 đến `max_choices`** phòng ngẫu nhiên trong danh sách → chọn 1.
3. Schematic phòng được dán ngay sau cửa. Phòng chiến đấu (`normal`, `elite`, `boss`) **đóng cửa sau 5 giây** (ai đứng ngoài bị kéo vào) cho tới khi phòng sạch.
4. Hoàn thành mọi objective của phòng → phòng sạch: mở cửa, xoá mob còn lại, tắt spawner tuỳ chỉnh trong phòng, bắn sự kiện `room_cleared`. Phòng không có objective sạch ngay khi vào.
5. Phòng `boss` chỉ được chọn sau khi đã qua **`minimum_rooms`** phòng. Trong phòng boss, tương tác **trial spawner** → xác nhận → triệu hồi mob `boss`.
6. Objective có `ends_dungeon: true` hoàn thành → bắn `boss_defeated`; không mở thêm cửa nào nữa.

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `minimum_rooms` | `3` | Số phòng phải qua trước khi phòng boss xuất hiện trong lựa chọn. |
| `max_choices` | `3` | Số lựa chọn tối đa mỗi lần mở cửa (ngẫu nhiên 1..n). |
| `pools[].min`, `max` | `1`, `1` | Số phòng rút từ pool. |
| `pools[].rooms[].room` | bắt buộc | Id phòng trong `dungeon/rooms/`. |
| `pools[].rooms[].weight` | `1` | Trọng số rút. |

Phòng loại `spawn` và `treasure` không bao giờ hiện trong lựa chọn cửa.

### `revive` — gục và hồi sinh

```json
"revive": { "radius": 3, "revive_seconds": 12, "revive_health": 5, "fail_on_wipe": true }
```

Người chơi chết trong instance **không chết thật**: họ gục tại chỗ, chuyển sang chế độ quan sát (bị giữ trong bán kính `spectator_radius` quanh điểm gục), để lại 1 hình nộm phát sáng. Đồng đội **ngồi (sneak) trong bán kính `radius`** quanh hình nộm để hồi sinh; tiến độ tăng 1 mỗi giây có người cứu, giảm 1 mỗi giây không có ai. Đủ `revive_seconds` → hồi sinh với `revive_health` máu.

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `radius` | `3` | Khoảng cách cứu (block). |
| `revive_seconds` | `12` | Số giây cần để hồi sinh. |
| `revive_health` | `5` | Máu sau khi hồi sinh. |
| `revive_on_room_clear` | `true` | Dọn sạch 1 phòng → hồi sinh mọi người đang gục. |
| `fail_on_wipe` | `true` | Mọi người trong instance đều gục → instance sụp đổ. |
| `wipe_delay_seconds` | `10` | Thời gian chờ trước khi sụp đổ (hồi sinh được ai trong lúc này thì huỷ). |
| `spectator_radius` | `16` | Người gục bị kéo lại nếu bay xa hơn. |

### `reward_chests` — rương thưởng

```json
"reward_chests": { "loot_table": "crypt_chest" }
```

Rương thưởng được đặt sẵn trong template/schematic bằng item từ `/dungeonbuild rewardchest <bảng>`. Mỗi người mở được **1 lần** và nhận loot **riêng** (roll theo người mở; rương đôi roll 2 lần). Không phá được. Rương đánh dấu bảng `dungeon` dùng `loot_table` của feature này — tiện khi cùng 1 schematic dùng cho nhiều dungeon với loot khác nhau.

### `treasure` — phòng kho báu sau boss

```json
"treasure": { "rooms": ["crypt_treasure"], "extend_seconds": 300, "spacing": 32 }
```

Cần feature `rooms`. Khi instance bắt đầu, chọn ngẫu nhiên 1 phòng trong `rooms`. Sau `boss_defeated`:
- Hạn của instance được **đặt lại** thành `extend_seconds` giây kể từ lúc đó (cần `expiry`).
- Tương tác vault → cánh cửa biến thành cổng end gateway.
- Bước vào cổng → mỗi người được dán **1 bản phòng kho báu riêng** phía trên spawn (cách nhau `spacing` block).
- Đi vào cổng nether trong phòng kho báu → về hub. Người cuối cùng ra → instance kết thúc ("đã được chinh phục").

## Phòng

File trong `dungeon/rooms/`, ví dụ `dungeon/rooms/crypt_boss.json`:

```json
{
  "type": "boss",
  "name": "Ngai vàng Hellknight",
  "schematic": "crypt_boss",
  "boss": "hellknight",
  "objectives": [
    { "on": { "event": "mob_killed", "subject": "hellknight" }, "ends_dungeon": true, "score": 50, "display": "Hạ Hellknight" }
  ],
  "effects": [ { "type": "strength_buff", "amplifier": 1 } ]
}
```

| Key | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `type` | `normal` | Loại phòng (bảng dưới). Quyết định icon trong menu chọn phòng và cách xử lý. |
| `name` | id phòng | Tên hiển thị trong menu chọn phòng và scoreboard. |
| `schematic` | bắt buộc | Tên file trong `dungeon/schematics/` (không kèm `.schem`). |
| `boss` | `""` | Id mob triệu hồi khi tương tác trial spawner (chỉ phòng `boss`). |
| `objectives` | `[]` | Điều kiện để phòng sạch. |
| `effects` | `[]` | Hiệu ứng khi đang ở trong phòng. |

| `type` | Chiến đấu (đóng cửa) | Ghi chú |
| :--- | :--- | :--- |
| `normal`, `elite` | có | Phòng đánh thường / khó. |
| `boss` | có | Chỉ mở sau `minimum_rooms`; triệu hồi boss bằng trial spawner. |
| `spawn`, `treasure` | không | Không xuất hiện ở cửa. |
| `warmup`, `checkpoint`, `safe`, `trade`, `puzzle`, `trap`, `loot`, `event` | không | Nhãn cho designer và condition `room_type`; hành vi do objective/trigger quyết định. |

### Objective phòng

```json
{ "on": { "event": "mob_killed" }, "count": 20, "score": 10, "display": "Tiêu diệt quái" }
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `on` | bắt buộc | [Matcher](#matcher) — sự kiện nào được tính. |
| `conditions` | `[]` | [Condition](#condition) phải đúng thì mới tính. |
| `count` | `1` | Số lần cần. Sự kiện có `amount` (vd. nhặt 5 item) cộng theo số lượng. |
| `score` | `0` | Điểm cộng khi hoàn thành (hiện trên scoreboard, có trong `room_cleared`). |
| `ends_dungeon` | `false` | Hoàn thành → boss bị hạ (`boss_defeated`). |
| `display` | `Mục tiêu N` | Nhãn trên scoreboard. |

Sự kiện có vị trí (giết mob, phá block, nhặt đồ...) **chỉ được tính khi xảy ra trong phòng hiện tại**.

### Effect phòng

Bắt đầu khi vào phòng, dừng khi phòng sạch. Chỉ áp lên người chơi đang ở trong phòng.

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `darkness` | `{ "type": "darkness", "flicker": true }` | Mù liên tục. `flicker: true` = chu kỳ 20 giây, sáng lại từ giây 10 đến 15. |
| `creeping_dread` | `{ "type": "creeping_dread", "max_triggers": 4, "spawn_chance": 0.25, "min_interval": 40, "max_interval": 100 }` | Mỗi người nghe tiếng creeper sắp nổ quanh mình, cách nhau `min_interval`–`max_interval` **giây**, tối đa `max_triggers` lần; mỗi lần có `spawn_chance` sinh creeper thật sau lưng. |
| `strength_buff` | `{ "type": "strength_buff", "duration_ticks": 3600, "amplifier": 1 }` | Tăng sát thương cho người trong phòng khi vào phòng. |

## Script

Script là phần "lập trình bằng JSON" của dungeon: **khi sự kiện X xảy ra, nếu điều kiện Y đúng, thì làm Z**. Dùng nó cho cốt truyện, câu đố, sự kiện boss. Logic phức tạp (nhiều state theo người chơi, thời gian chính xác) nên viết thành feature bằng code.

### Trigger

```json
{
  "on": { "event": "item_picked_up", "subject": "crypt_key" },
  "once": true,
  "conditions": [ { "type": "var", "var": "keys", "value": { "min": 3 } } ],
  "actions": [
    { "type": "set_var", "var": "gate_open", "value": true },
    { "type": "message", "text": "<green>Cổng đã mở!", "all": true }
  ]
}
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `on` | bắt buộc | [Matcher](#matcher). |
| `conditions` | `[]` | Tất cả phải đúng. |
| `once` | `false` | Chỉ chạy 1 lần mỗi instance. |
| `actions` | bắt buộc | Chạy theo thứ tự. |

Trigger được xét **theo thứ tự trong danh sách**; action của trigger trước đã chạy xong khi trigger sau được xét (vd. trigger 1 `add_var`, trigger 2 kiểm tra biến đó thấy giá trị mới).

`once` được lưu thành biến tên `<key feature>.<chỉ số trigger>` (vd. `triggers.2`), có thể đọc hoặc gán lại bằng `set_var` để "mở khoá" lại trigger.

**Đếm số lần** (thay cho `count` cũ): dùng biến.
```json
{ "on": { "event": "mob_killed", "subject": "skeleton_guard" },
  "actions": [ { "type": "add_var", "var": "guards", "amount": 1 } ] },
{ "on": { "event": "mob_killed", "subject": "skeleton_guard" },
  "once": true,
  "conditions": [ { "type": "var", "var": "guards", "value": { "min": 10 } } ],
  "actions": [ { "type": "signal", "id": "gate_open" } ] }
```

### Sự kiện

| Sự kiện | Khi nào | `subject` | `data` |
| :--- | :--- | :--- | :--- |
| `started` | Instance vừa được tạo | | |
| `player_entered` | Người chơi vào world instance | | |
| `player_left` | Người chơi rời world / thoát game | | |
| `region_entered`, `region_left` | Vào/rời region WorldGuard | id region | |
| `mob_killed` | Người chơi giết mob | id mob (tuỳ chỉnh hoặc vanilla) | |
| `mob_died` | Mob chết vì bất kỳ lý do gì (người chơi, môi trường, ...) | id mob | `tag` (nếu mob được sinh bằng `spawn_mob` có `tag`) |
| `mob_damaged` | Mob nhận sát thương | id mob | `damage`, `health_percent` (0–100, sau đòn) |
| `block_interacted` | Chuột phải block | id block (`oak_door`) | |
| `block_broken` | Phá block | id block | |
| `item_picked_up` | Nhặt item | id item (tuỳ chỉnh hoặc vanilla) | `amount` |
| `portal_entered` | Bước vào block cổng | id block cổng | |
| `player_died` | Người chơi chết (kể cả khi `revive` giữ lại) | | |
| `quest_completed` | Quest nhóm của instance hoàn thành | id quest | |
| `room_started` | Vào phòng mới (`rooms`) | | `room`, `type`, `cleared`, `score` |
| `room_cleared` | Phòng sạch | | `room`, `type`, `cleared`, `score` |
| `boss_defeated` | Objective `ends_dungeon` hoàn thành | | |
| *(tự đặt)* | Action `signal` hoặc lệnh admin | | dữ liệu kèm theo |

Sự kiện do người chơi gây ra mang theo người đó (dùng cho `all: false` và `{player}`). Sự kiện bị feature khác **huỷ** (vd. bấm vault để mở cửa phòng, phá rương thưởng) không khớp matcher nào.

### Matcher

Dạng ngắn — chỉ id sự kiện:
```json
"on": "started"
```

Dạng đầy đủ:
```json
"on": {
  "event": "room_cleared",
  "subject": "crypt_boss",
  "data": { "type": { "in": ["elite", "boss"] }, "cleared": { "min": 3 } }
}
```

| Key | Ý nghĩa |
| :--- | :--- |
| `event` | Id sự kiện. |
| `subject` | (tuỳ chọn) [Điều kiện giá trị](#điều-kiện-giá-trị) trên subject. |
| `data` | (tuỳ chọn) Mỗi khoá → điều kiện giá trị; tất cả phải khớp. |

### Điều kiện giá trị

Dùng ở `subject`, `data` và trong condition. Viết thẳng giá trị nghĩa là "bằng".

| Dạng | Khớp khi | Ví dụ |
| :--- | :--- | :--- |
| giá trị | Bằng | `"boss"`, `3`, `true` |
| `in` | Bằng 1 trong các giá trị | `{ "in": ["elite", "boss"] }` |
| `min` / `max` | Số ≥ min và/hoặc ≤ max | `{ "min": 3 }`, `{ "min": 1, "max": 5 }` |
| `range` | Số trong khoảng (gồm 2 đầu) | `{ "range": [20, 60] }` |
| `not` | Điều kiện bên trong sai | `{ "not": "south" }` |
| `any_of` | Một trong các điều kiện đúng | `{ "any_of": [ "a", { "min": 10 } ] }` |
| `exists` | Có / không có giá trị | `{ "exists": true }` |

So sánh số dùng giá trị thực: `3` và `3.0` là như nhau. Giá trị không tồn tại (biến chưa gán, khoá không có) chỉ khớp `{ "exists": false }` và các dạng `not`.

### Condition

Kiểm tra **trạng thái instance**, không phải sự kiện.

| `type` | Ví dụ | Đúng khi |
| :--- | :--- | :--- |
| `var` | `{ "type": "var", "var": "keys", "value": { "min": 3 } }` | Biến khớp điều kiện. |
| `room_type` | `{ "type": "room_type", "value": "boss" }` | Loại phòng hiện tại khớp (cần `rooms`). |
| `members` | `{ "type": "members", "value": { "max": 2 } }` | Số thành viên khớp. |
| `mob_count` | `{ "type": "mob_count", "tag": "gate_guard", "count": { "max": 1 } }` | Số mob mang `tag` đang có trong world instance (chunk đã nạp) khớp điều kiện. |
| `time_left` | `{ "type": "time_left", "value": { "min": 600 } }` | Số giây còn lại khớp (cần `expiry`). |
| `all_of` | `{ "type": "all_of", "conditions": [ ... ] }` | Tất cả đúng. |
| `any_of` | `{ "type": "any_of", "conditions": [ ... ] }` | Ít nhất 1 đúng. |
| `not` | `{ "type": "not", "condition": { ... } }` | Condition bên trong sai. |

### Action

`all: false` (mặc định) nghĩa là chỉ áp lên **người gây ra sự kiện**; nếu sự kiện không có người (vd. `started`, `room_cleared`) thì áp lên mọi người đang trong instance. `all: true` luôn áp lên mọi người.

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `message` | `{ "type": "message", "text": "<yellow>{player} tìm thấy chìa ({var.keys}/3)", "all": true }` | Gửi tin nhắn. |
| `title` | `{ "type": "title", "title": "<gold>Chiến thắng!", "subtitle": "<gray>Kho báu đã mở", "all": true }` | Hiện title. `fade_in` 10, `stay` 70, `fade_out` 20 (tick). |
| `play_sound` | `{ "type": "play_sound", "sound": "minecraft:entity.wither.spawn", "volume": 1, "pitch": 1, "all": true }` | Phát âm thanh tại người nghe. |
| `teleport` | `{ "type": "teleport", "x": 120.5, "y": 64, "z": -40.5, "yaw": 90, "all": true }` | Dịch chuyển trong world instance. |
| `give_item` | `{ "type": "give_item", "item": "crypt_relic", "amount": 1 }` | Trao item. Id tuỳ chỉnh trước, rồi tới vật liệu vanilla. Túi đầy thì rơi xuống đất (chỉ người nhận thấy). |
| `spawn_mob` | `{ "type": "spawn_mob", "mob": "skeleton_guard", "count": 4, "x": 130, "y": 64, "z": -50, "spread": 4 }` | Sinh mob, rải ngẫu nhiên trong bán kính `spread`. Id tuỳ chỉnh trước, rồi vanilla. `tag` (tuỳ chọn) để theo dõi mob, xem [Mob được theo dõi](#mob-được-theo-dõi). |
| `set_block` | `{ "type": "set_block", "x": 120, "y": 64, "z": -40, "block": "minecraft:oak_door[open=true]" }` | Đặt block, hỗ trợ block state. Sai cú pháp → lỗi khi nạp. |
| `set_var` | `{ "type": "set_var", "var": "gate_open", "value": true }` | Gán biến. |
| `add_var` | `{ "type": "add_var", "var": "keys", "amount": 1 }` | Cộng vào biến số (chưa có = 0). Dùng `amount` âm để trừ. |
| `signal` | `{ "type": "signal", "id": "gate_open", "amount": 1, "data": { "gate": "north" } }` | Bắn sự kiện tự đặt. Trigger khác bắt bằng `"on": { "event": "gate_open" }`; quest nhóm có objective `signal` cùng id cũng tiến `amount`. |
| `start_quest` | `{ "type": "start_quest", "quest": "crypt_epilogue" }` | Giao quest cho nhóm quest của instance, mọi thành viên được nhận thưởng. Cần feature `quests`. |
| `sequence` | xem dưới | Chạy action lần lượt, có thể chờ. |
| `delay` | `{ "type": "delay", "ticks": 60 }` | Chỉ có tác dụng trong `sequence`. Tính bằng tick (20 tick = 1 giây). |
| `repeat` | xem dưới | Lặp lại action. |
| `if` | xem dưới | Rẽ nhánh theo condition. |
| `end` | `{ "type": "end", "reason": "đã kết thúc" }` | Kết thúc instance, báo `reason` cho thành viên. |

```json
{ "type": "sequence", "steps": [
  { "type": "message", "text": "<red>Mặt đất rung chuyển...", "all": true },
  { "type": "delay", "ticks": 60 },
  { "type": "set_block", "x": 120, "y": 64, "z": -40, "block": "minecraft:air" },
  { "type": "signal", "id": "gate_open" }
] }
```

Sau mỗi `delay`, phần còn lại chỉ chạy nếu instance chưa kết thúc. Người gây sự kiện đã thoát thì phần còn lại chạy như không có người.

#### `repeat`

```json
{ "type": "repeat", "times": 3, "interval": 400, "while": [ ... ], "actions": [ ... ] }
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `times` | *(bắt buộc)* | Số lần lặp tối đa. |
| `while` | `[]` | [Condition](#condition) kiểm tra trước mỗi lần lặp; sai thì dừng. |
| `interval` | `0` | Số tick chờ giữa các lần lặp. `0` = chạy hết ngay trong tick hiện tại. Khi chờ, giống `delay`: dừng nếu instance đã kết thúc. |
| `actions` | *(bắt buộc)* | Action chạy mỗi lần lặp. |

#### `if`

```json
{ "type": "if", "conditions": [ ... ], "then": [ ... ], "else": [ ... ] }
```

Tất cả `conditions` đúng → chạy `then`, ngược lại chạy `else`. `then` và `else` đều tuỳ chọn.

#### Mob được theo dõi

`spawn_mob` có `tag` vẫn sinh đủ `count` mob mỗi lần chạy; mob được gắn `tag` và không tự biến mất khi ở xa. Plugin **không lưu** gì thêm: condition `mob_count` đếm số mob mang `tag` đang thực sự có trong world, nên mob bị despawn hoặc mất sau restart tự động không còn được đếm. Mob mang `tag` chết vì bất kỳ lý do gì sẽ bắn `mob_died` kèm `tag` — tự đếm bằng biến nếu cần.

Ví dụ: 2 lính gác, mỗi lính 1 `tag` riêng. Vào lại khu vực thì sinh lại lính bị mất (despawn, restart), nhưng lính đã chết thì không bao giờ quay lại:

```json
"variables": { "guard_1_dead": false, "guard_2_dead": false },
"features": {
  "triggers": { "triggers": [
    { "on": { "event": "mob_died", "data": { "tag": "gate_guard_1" } },
      "actions": [ { "type": "set_var", "var": "guard_1_dead", "value": true } ] },
    { "on": { "event": "mob_died", "data": { "tag": "gate_guard_2" } },
      "actions": [ { "type": "set_var", "var": "guard_2_dead", "value": true } ] },
    { "on": { "event": "region_entered", "subject": "gate_room" },
      "actions": [
        { "type": "if",
          "conditions": [
            { "type": "var", "var": "guard_1_dead", "value": false },
            { "type": "mob_count", "tag": "gate_guard_1", "count": 0 } ],
          "then": [ { "type": "spawn_mob", "mob": "vein_stalker", "x": 10, "y": 64, "z": 5, "tag": "gate_guard_1" } ] },
        { "type": "if",
          "conditions": [
            { "type": "var", "var": "guard_2_dead", "value": false },
            { "type": "mob_count", "tag": "gate_guard_2", "count": 0 } ],
          "then": [ { "type": "spawn_mob", "mob": "vein_stalker", "x": 14, "y": 64, "z": 5, "tag": "gate_guard_2" } ] }
      ] }
  ] }
}
```

Trigger vào khu vực **không** dùng `once`, để mỗi lần vào lại đều kiểm tra và bù lính. Khai báo biến ở `variables`: biến chưa tồn tại không khớp `false`.

Objective phòng tính cả khi lính chết không do người chơi:

```json
{ "on": { "event": "mob_died", "data": { "tag": { "in": ["gate_guard_1", "gate_guard_2"] } } }, "count": 2 }
```

Muốn giữ 1 nhóm mob với số lượng cố định (không quan tâm từng con), dùng `repeat` + `mob_count`:

```json
{ "type": "repeat", "times": 5,
  "while": [ { "type": "mob_count", "tag": "patrol", "count": { "max": 4 } } ],
  "actions": [ { "type": "spawn_mob", "mob": "minecraft:zombie", "x": 0, "y": 70, "z": 0, "spread": 4, "tag": "patrol" } ] }
```

Text trong `message` và `title` hỗ trợ:
- `{player}` — tên người gây sự kiện.
- `{var.<tên>}` — giá trị biến (số nguyên hiện không có `.0`).

### Biến

Biến là giá trị **số, chuỗi hoặc true/false** lưu theo từng instance (còn nguyên sau khi restart server). Dùng để đếm, đánh dấu tiến độ câu đố, nhớ lựa chọn của người chơi.

- Khai báo giá trị ban đầu ở `variables` của file dungeon. Instance cũ nhận thêm biến mới khai báo khi được nạp lại.
- Ghi: `set_var`, `add_var`, và `once` của trigger.
- Đọc: condition `var`, `{var.<tên>}` trong text.

## Kiểm tra khi nạp

Sau mỗi lần nạp (khởi động hoặc `/smpreload` toàn bộ), mọi dungeon được kiểm tra. Lỗi **chỉ log cảnh báo**, dungeon vẫn nạp. Mỗi dòng có đường dẫn tới chỗ sai:

```
Dungeon crypt: features.triggers.triggers[3].on: event 'gate_opn' is never fired by this dungeon
Dungeon crypt: features.triggers.triggers[1].actions[1]: variable 'kyes' is never declared or set
Dungeon crypt: features.rooms.room 'crypt_boss'.objectives[0].on: event 'mob_damaged' never carries data 'hp'
Dungeon crypt: features.reward_chests.loot_table: unknown loot table 'crypt_chst'
```

Được kiểm tra:
- Sự kiện mà matcher nghe nhưng không có gì bắn (không phải built-in, không có `signal` nào trong dungeon gửi).
- Khoá `data` mà sự kiện không bao giờ mang.
- Biến được đọc nhưng chưa khai báo và không có action nào gán.
- Mob, item, phòng, schematic, loot table không tồn tại.

Sự kiện chỉ được bắn từ code hoặc lệnh admin sẽ bị báo nhầm là "never fired" — bỏ qua cảnh báo đó.

## Lệnh

| Lệnh | Ai dùng | Tác dụng |
| :--- | :--- | :--- |
| `/dungeonrun` (`/drun`) | người chơi | Mở menu: danh sách dungeon, tạo, vào lại, lời mời, mời người khác, rời bỏ. |
| `/dungeonrun create <dungeon>` | người chơi | Tạo instance. |
| `/dungeonrun join <dungeon> [người mời]` | người chơi | Vào instance của mình, hoặc nhận lời mời. |
| `/dungeonrun invite <người> [dungeon]` | người chơi | Mời người chơi vào instance (đứng trong instance hoặc ghi tên dungeon). |
| `/dungeonrun abandon [dungeon]`, `leave`, `list` | người chơi | Rời bỏ / ra hub / liệt kê instance của mình. |
| `/dungeonrun admin list\|info\|extend\|end\|addmember\|removemember\|signal` | `roguesmp.dungeon.admin` | Quản lý instance; `signal` bắn sự kiện tự đặt để thử script. |
| `/dungeonbuild schematic save <tên>` | `roguesmp.dungeon.build` | Lưu vùng chọn WorldEdit thành schematic phòng; vị trí đứng là gốc dán. |
| `/dungeonbuild schematic paste <tên>` | | Dán schematic tại chỗ đứng để kiểm tra. |
| `/dungeonbuild rewardchest <bảng>` | | Lấy rương thưởng gắn loot table (`dungeon` = dùng bảng của feature). |
| `/dungeonbuild loottable <bảng>` | | Roll thử loot table vào túi. |

Spawner tuỳ chỉnh là hệ riêng: [JSON-Spawners](JSON-Spawners.md).

---
◀ [JSON-Blocks](JSON-Blocks.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Spawners](JSON-Spawners.md)
