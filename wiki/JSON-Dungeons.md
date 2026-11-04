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
"triggers": { "triggers": [ { "on": "started", "do": [ ... ] } ] }
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

### `block_palettes` — khối ngẫu nhiên

Định nghĩa các palette mà khối đánh dấu [`palette`](JSON-Blocks.md#palette--khối-ngẫu-nhiên-cho-dungeon) dùng. Mỗi khối đánh dấu trong world của instance được thay bằng 1 block rút từ palette cùng tên.

```json
"block_palettes": {
  "palettes": {
    "cracked_wall": {
      "chance": 0.8,
      "fallback": "minecraft:stone_bricks",
      "entries": [
        { "block": "minecraft:cracked_stone_bricks", "weight": 3 },
        { "block": "minecraft:mossy_stone_bricks", "weight": 1 }
      ]
    },
    "floor": {
      "entries": [
        { "block": "minecraft:polished_andesite", "weight": 4 },
        { "block": "minecraft:oak_slab[type=bottom]" },
        { "block": "dungeon_loot_urn" }
      ]
    }
  }
}
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `palettes` | bắt buộc | Map tên palette → palette. |
| `palettes.<tên>.entries` | bắt buộc | Danh sách block có thể ra. |
| `entries[].block` | bắt buộc | Có tiền tố `minecraft:` = block vanilla (ghi được block state, vd `minecraft:oak_stairs[facing=north]`). Không tiền tố = id block tuỳ chỉnh trong `blocks/`. |
| `entries[].weight` | `1` | Trọng số rút. |
| `palettes.<tên>.chance` | `1` | Xác suất được thay bằng 1 entry. Trượt → dùng `fallback`. |
| `palettes.<tên>.fallback` | không có | Block dùng khi trượt `chance` (cùng cú pháp `block`). Không có → không khí. |

Khi nào thay:
- **Khối trong template**: ngay khi instance bắt đầu (và khi khôi phục sau restart, để dọn những khối còn sót).
- **Khối trong schematic phòng**: ngay khi phòng được dán.
- Kết quả cố định theo instance + vị trí: cùng 1 lượt chơi, cùng 1 ô luôn ra cùng block.

Kiểm tra khi nạp: báo block tuỳ chỉnh không tồn tại, hoặc entry/fallback lại là 1 khối `palette`. Block vanilla viết sai thì dungeon báo lỗi ngay khi đọc. Palette mà khối đánh dấu cần nhưng dungeon không có **không** được kiểm tra trước (không biết trước khối nào nằm trong world) — chỉ log khi gặp.

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
{ "on": "item_picked_up crypt_key", "once": true, "if": "v.keys >= 3",
  "do": [ "v.gate_open = true", { "type": "message", "text": "<green>Cổng đã mở!", "all": true } ] }
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `on` | | [Matcher](#matcher): chạy khi sự kiện xảy ra. |
| `when` | | [Biểu thức](#biểu-thức): chạy khi biểu thức chuyển từ sai sang đúng. Cần đúng 1 trong `on` / `when`. |
| `if` (hoặc `conditions`) | `[]` | [Condition](#condition), 1 cái hoặc danh sách; tất cả phải đúng. |
| `once` | `false` | Chỉ chạy 1 lần mỗi instance. |
| `cooldown` | `0` | Số tick tối thiểu giữa 2 lần chạy (20 tick = 1 giây; 0 = không giới hạn). |
| `interval` | `20` | Chỉ cho sự kiện `timer`: chạy mỗi `interval` tick. |
| `recheck` | `0` | Chỉ cho `when`: kiểm tra lại biểu thức mỗi `recheck` tick (0 = chỉ khi biến đổi). Cần khi `when` dùng query. |
| `do` (hoặc `actions`) | bắt buộc | [Action](#action), 1 cái hoặc danh sách; chạy theo thứ tự. |

Trigger được xét **theo thứ tự trong danh sách**; action của trigger trước đã chạy xong khi trigger sau được xét (vd. trigger 1 `add_var`, trigger 2 kiểm tra biến đó thấy giá trị mới).

`once` được lưu thành biến tên `<key feature>.<chỉ số trigger>` (vd. `triggers.2`), có thể đọc hoặc gán lại bằng `set_var` để "mở khoá" lại trigger.

#### Trigger `when`

```json
{ "when": "v.lever_a && v.lever_b && v.lever_c", "once": true, "do": [ ... ] }
```

- Biểu thức được kiểm tra lại **mỗi khi 1 biến `v.` mà nó đọc thay đổi**, và khi instance bắt đầu / được nạp lại. Query (`q.`) không tự kích hoạt kiểm tra lại: thêm `"recheck": <tick>` để kiểm tra định kỳ, vd. `{ "when": "q.mob_count('boss') == 0", "recheck": 20, ... }`.
- Chỉ chạy khi kết quả **chuyển từ sai sang đúng**. Đúng rồi đúng tiếp thì không chạy lại; sai rồi đúng lại thì chạy lại (trừ khi có `once`).
- Không có sự kiện kích hoạt: không dùng được `c.`, `p.` và query cần người chơi.
- Trạng thái lưu ở biến `<key feature>.<chỉ số>.when`.

**Đếm số lần**: dùng biến.
```json
{ "on": "mob_killed skeleton_guard", "do": [ "v.guards += 1" ] },
{ "when": "v.guards >= 10", "once": true, "do": [ { "type": "signal", "id": "gate_open" } ] }
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
| `block_interacted` | Chuột phải block | id block (`minecraft:oak_door`, hoặc id block tuỳ chỉnh) | `x`, `y`, `z` (toạ độ block) |
| `block_broken` | Phá block | id block (`minecraft:…` hoặc tuỳ chỉnh) | `x`, `y`, `z` (toạ độ block) |
| `block_placed` | Đặt block (cả block tuỳ chỉnh) | id block (`minecraft:…` hoặc tuỳ chỉnh) | `x`, `y`, `z` (toạ độ block) |
| `item_picked_up` | Nhặt item | id item tuỳ chỉnh, hoặc `minecraft:<vật liệu>` cho vanilla | `amount` |
| `portal_entered` | Bước vào block cổng | id block cổng | |
| `player_died` | Người chơi chết (kể cả khi `revive` giữ lại) | | |
| `quest_completed` | Quest nhóm của instance hoàn thành | id quest | |
| `redstone_activated` | Bấm nút, gạt cần, dẫm tấm áp lực / dây bẫy (1 lần mỗi lần kích hoạt, không lặp khi đứng yên) | id block (`minecraft:…`) | `x`, `y`, `z`, `powered` (cần gạt: trạng thái sau khi gạt; còn lại luôn `true`) |
| `var_changed` | Biến đổi giá trị qua `set_var` / `add_var` | tên biến | `value`, `old` (nếu đã có giá trị trước) |
| `countdown_ended` | Đếm ngược của action `countdown` hết giờ | id đếm ngược | |
| `timer` | Mỗi `interval` tick của trigger, khi có người trong instance | | `ticks` (game time của world instance) |
| `room_started` | Vào phòng mới (`rooms`) | | `room`, `type`, `cleared`, `score` |
| `room_cleared` | Phòng sạch | | `room`, `type`, `cleared`, `score` |
| `boss_defeated` | Objective `ends_dungeon` hoàn thành | | |
| *(tự đặt)* | Action `signal` hoặc lệnh admin | | dữ liệu kèm theo |
| `player_fished` | Câu cá (mọi giai đoạn) | trạng thái (`fishing`, `caught_fish`, `caught_entity`, `in_ground`, `failed_attempt`, `reel_in`, `bite`, ...) | `exp`, `caught` |
| `item_dropped` | Vứt item | id item | `amount` |
| `item_consumed` | Ăn / uống item | id item | |
| `player_sneaked` | Bật / tắt ngồi | | `sneaking` |
| `entity_interacted` | Chuột phải entity | id mob (tuỳ chỉnh hoặc vanilla) | |
| `ability_cast` | Dùng kỹ năng | id kỹ năng | `level` |

Mọi sự kiện đến từ Bukkit (trừ `started`, `var_changed`, `countdown_ended`, `timer`, sự kiện phòng và sự kiện tự đặt) được khai báo bằng `BukkitEventAdapter` trong `BukkitEventAdapters` (code). 6 sự kiện cuối bảng chỉ dành cho script nên chỉ được lắng nghe khi có dungeon dùng tới.

Sự kiện do người chơi gây ra mang theo người đó (dùng cho `all: false`, `{player}`, `p.` và query người chơi). Sự kiện bị feature khác hoặc action `cancel` **huỷ** (vd. bấm vault để mở cửa phòng, phá rương thưởng) không khớp matcher nào phía sau.

Huỷ được bằng action `cancel`: `block_interacted`, `block_broken`, `block_placed`, và các sự kiện Bukkit huỷ được (`player_fished`, `item_dropped`, `item_consumed`, `entity_interacted`, `ability_cast`).

### Matcher

Dạng chuỗi:
```
<sự kiện> [subject] [@ x y z] [in <region>] [khoá=giá trị ...]
```
```json
"on": "started"
"on": "mob_killed hellknight"
"on": "block_placed minecraft:gold_block in altar"
"on": "redstone_activated @ 50 64 10"
"on": "mob_died tag=gate_guard_1"
"on": "var_changed altar_gold value=4"
```

`@ x y z` là viết tắt của `x=… y=… z=…`. Giá trị `true`/`false`/số được hiểu đúng kiểu, còn lại là chuỗi. So sánh khác "bằng" thì dùng dạng đầy đủ hoặc `if`.

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
| `region` | (tuỳ chọn) Id region WorldGuard; vị trí sự kiện (block, mob, người chơi) phải nằm trong region. |

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

### Biểu thức

Biểu thức kiểu [Molang](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/): chuỗi JSON, chuỗi bên trong dùng nháy đơn `'`. Được kiểm tra cú pháp khi nạp; sai → dungeon không nạp được và log báo vị trí lỗi.

```json
"if": "v.keys >= 3 && q.has_item('crypt_key')"
"text": "<yellow>Bệ thờ {v.altar_gold}/4"
"do": [ "v.altar_gold += 1; t.bonus = math.floor(v.score / 10)" ]
```

| Tiền tố | Ý nghĩa |
| :--- | :--- |
| `v.` / `variable.` / `var.` | [Biến](#biến) của instance. |
| `p.` / `pvar.` | Biến riêng của người gây sự kiện (không có người → biến chung). |
| `t.` / `temp.` | Biến tạm, chỉ sống trong 1 lần chạy trigger. |
| `c.` / `context.` | Dữ liệu của sự kiện kích hoạt (chỉ đọc). Luôn có `c.player` (tên), `c.subject`, `c.event`, cộng các khoá `data` của sự kiện (vd. `c.powered`, `c.x`). Trong function có `params` thì là tham số. |
| `q.` / `query.` | Đọc trạng thái game, xem bảng dưới. |
| `math.` | Hàm toán học. |

Tiền tố và tên hàm không phân biệt hoa thường; tên biến thì có.

- Phép toán: `+ - * / %`, `== != < <= > >=`, `&& || !`, `điều_kiện ? a : b`, ngoặc `( )`. `+` có chuỗi → nối chuỗi.
- `true` / `false`. Đúng/sai: số khác 0, `true`, chuỗi không rỗng là đúng.
- **Thứ chưa có giá trị là `0`** (biến chưa gán, khoá `c.` không có, query không áp dụng). `true + true = 2`.
- Chuỗi chỉ so sánh `==` / `!=`: `v.door == 'open'`.
- `a ?? b`: dùng `b` khi `a` **chưa có giá trị** (biến chưa gán, khoá `c.` không có, query không áp dụng). Vd. `{v.rank ?? 'Chưa có'}`, `q.time_left ?? 999`. Khác với `0`: biến đã gán `0` vẫn là `0`.

**Gán** chỉ có trong action dạng chuỗi (xem [Action](#action)): `=`, `+=`, `-=`, `*=`, `/=`, nhiều lệnh cách nhau bằng `;`. Gán `v.`/`p.` giống `set_var` (bắn `var_changed`). Biểu thức ở chỗ khác (điều kiện, text) không gán được.

| Query | Trả về |
| :--- | :--- |
| `q.alive_count` | Số người còn sống (không chết, không spectator). |
| `q.members` | Số thành viên. |
| `q.player_count` | Số người đang trong world instance. |
| `q.time_left` | Số giây còn lại (cần `expiry`, không có → 0). |
| `q.room_type` | Loại phòng hiện tại (cần `rooms`). |
| `q.mob_count('tag')` | Số mob mang `tag` còn sống. |
| `q.in_region('id')` / `q.in_region('id', 'all')` | Người gây sự kiện / mọi người đứng trong region. |
| `q.has_item('item')` / `q.has_item('item', 3)` | Người gây sự kiện có ít nhất N item. |
| `q.item_count('item')` | Số item trong túi người gây sự kiện. |
| `q.player_health` | % máu (0–100) người gây sự kiện. |
| `q.player_name` | Tên người gây sự kiện. |

Query cần người chơi trả `0` khi không có người.

| Hàm | |
| :--- | :--- |
| `math.abs(x)`, `floor`, `ceil`, `round`, `trunc`, `sqrt` | 1 tham số. |
| `math.min(a, b)`, `max`, `pow`, `mod` | 2 tham số. |
| `math.clamp(x, min, max)`, `math.lerp(a, b, t)` | 3 tham số. |
| `math.random(a, b)` | Số thực ngẫu nhiên trong [a, b). |
| `math.random_integer(a, b)` | Số nguyên ngẫu nhiên trong [a, b]. |

**Giá trị có thể là biểu thức** ở `set_var.value`, `add_var.amount`, `signal.data`, `call.args`: chuỗi đọc được như biểu thức thì được tính khi chạy, chuỗi khác giữ nguyên là chữ (`"open"` là chữ, `"c.powered"` là biểu thức, `"5"` là số). Chuỗi trông như biểu thức nhưng sai cú pháp (vd. `"v.wave *"`) vẫn được dùng như chữ, kèm cảnh báo khi nạp.

**Số có thể là biểu thức** (chuỗi, tính khi action chạy; sai cú pháp → lỗi khi nạp): `delay.ticks`, `repeat.times` / `interval`, `spawn_mob.count` / `x` / `y` / `z` / `spread`, `give_item.amount`, `take_item.amount`, `effect.duration` / `amplifier`, `particles.x` / `y` / `z` / `count`, `teleport.x` / `y` / `z`, `countdown.ticks`. Vd. `{ "type": "spawn_mob", "mob": "minecraft:zombie", "count": "q.members * 2", "x": 0, "y": 64, "z": "10 + v.wave" }`. Số lẻ được làm tròn ở chỗ cần số nguyên.

### Condition

Kiểm tra **trạng thái instance** hoặc **người gây ra sự kiện** (`in_region`, `has_item`, `player_health`, biến `player`).

Condition viết dạng chuỗi là 1 [biểu thức](#biểu-thức): `"if": "v.keys >= 3"` giống `{ "type": "expr", "expr": "v.keys >= 3" }`. Các `type` dưới đây vẫn dùng được.

| `type` | Ví dụ | Đúng khi |
| :--- | :--- | :--- |
| `expr` | `{ "type": "expr", "expr": "v.keys >= 3" }` | Biểu thức đúng. |
| `var` | `{ "type": "var", "var": "keys", "value": { "min": 3 } }` | Biến khớp điều kiện. `"player": true` → đọc biến riêng của người gây sự kiện. |
| `room_type` | `{ "type": "room_type", "value": "boss" }` | Loại phòng hiện tại khớp (cần `rooms`). |
| `members` | `{ "type": "members", "value": { "max": 2 } }` | Số thành viên khớp. |
| `mob_count` | `{ "type": "mob_count", "tag": "gate_guard", "count": { "max": 1 } }` | Số mob mang `tag` đang có trong world instance (chunk đã nạp) khớp điều kiện. |
| `time_left` | `{ "type": "time_left", "value": { "min": 600 } }` | Số giây còn lại khớp (cần `expiry`). |
| `all_of` | `{ "type": "all_of", "conditions": [ ... ] }` | Tất cả đúng. |
| `any_of` | `{ "type": "any_of", "conditions": [ ... ] }` | Ít nhất 1 đúng. |
| `not` | `{ "type": "not", "condition": { ... } }` | Condition bên trong sai. |
| `in_region` | `{ "type": "in_region", "region": "altar", "all": false }` | Người gây sự kiện đứng trong region. `all: true` → mọi người trong instance đều đứng trong đó. |
| `has_item` | `{ "type": "has_item", "item": "crypt_key", "amount": 1 }` | Người gây sự kiện có ít nhất `amount` item (id tuỳ chỉnh hoặc vanilla) trong túi. |
| `player_health` | `{ "type": "player_health", "value": { "max": 30 } }` | % máu (0–100) của người gây sự kiện khớp. |
| `alive_count` | `{ "type": "alive_count", "value": 1 }` | Số người còn sống (không chết, không ở spectator) trong instance khớp. |
| `random` | `{ "type": "random", "chance": 0.25 }` | Ngẫu nhiên với xác suất `chance` (0–1). |

### Action

Action viết dạng chuỗi là lệnh gán [biểu thức](#biểu-thức): `"v.keys += 1"` giống `{ "type": "eval", "run": "v.keys += 1" }`. Danh sách action trộn được chuỗi và object.

`all: false` (mặc định) nghĩa là chỉ áp lên **người gây ra sự kiện**; nếu sự kiện không có người (vd. `started`, `room_cleared`) thì áp lên mọi người đang trong instance. `all: true` luôn áp lên mọi người.

| `type` | Ví dụ | Tác dụng |
| :--- | :--- | :--- |
| `message` | `{ "type": "message", "text": "<yellow>{player} tìm thấy chìa ({v.keys}/3)", "all": true }` | Gửi tin nhắn. |
| `title` | `{ "type": "title", "title": "<gold>Chiến thắng!", "subtitle": "<gray>Kho báu đã mở", "all": true }` | Hiện title. `fade_in` 10, `stay` 70, `fade_out` 20 (tick). |
| `play_sound` | `{ "type": "play_sound", "sound": "minecraft:entity.wither.spawn", "volume": 1, "pitch": 1, "all": true }` | Phát âm thanh tại người nghe. |
| `teleport` | `{ "type": "teleport", "x": 120.5, "y": 64, "z": -40.5, "yaw": 90, "all": true }` | Dịch chuyển trong world instance. |
| `give_item` | `{ "type": "give_item", "item": "crypt_relic", "amount": 1 }` | Trao item. Item vanilla phải có tiền tố `minecraft:` (`minecraft:diamond`), item tuỳ chỉnh dùng id trần. Túi đầy thì rơi xuống đất (chỉ người nhận thấy). |
| `spawn_mob` | `{ "type": "spawn_mob", "mob": "skeleton_guard", "count": 4, "x": 130, "y": 64, "z": -50, "spread": 4 }` | Sinh mob, rải ngẫu nhiên trong bán kính `spread`. Id tuỳ chỉnh trước, rồi vanilla. `tag` (tuỳ chọn) để theo dõi mob, xem [Mob được theo dõi](#mob-được-theo-dõi). |
| `set_block` | `{ "type": "set_block", "x": 120, "y": 64, "z": -40, "block": "minecraft:oak_door[open=true]" }` | Đặt block. Vanilla: `minecraft:<block>` (hỗ trợ block state, vd. `minecraft:oak_door[open=true]`); block tuỳ chỉnh: id trần. Block tuỳ chỉnh đang ở vị trí đó được gỡ đúng cách trước. Sai cú pháp → lỗi khi nạp. |
| `fill_block` | `{ "type": "fill_block", "from": [118, 64, -42], "to": [122, 67, -42], "block": "minecraft:air", "replace": "minecraft:iron_bars" }` | Lấp đầy khối hộp giữa hai góc bằng `block` (cùng cách viết id như `set_block`, hỗ trợ block tuỳ chỉnh). `replace` tuỳ chọn: chỉ thay block có id đó (`minecraft:iron_bars` hoặc id tuỳ chỉnh). Quá 32768 block → cảnh báo khi nạp. |
| `eval` | `{ "type": "eval", "run": "v.keys += 1; v.last = c.player" }` | Chạy lệnh gán. Thường viết thẳng dạng chuỗi. |
| `set_var` | `{ "type": "set_var", "var": "gate_open", "value": true }` | Gán biến. `value` có thể là biểu thức. |
| `add_var` | `{ "type": "add_var", "var": "keys", "amount": 1 }` | Cộng vào biến số (chưa có = 0). Dùng `amount` âm để trừ; `amount` có thể là biểu thức. `"player": true` → biến riêng của người gây sự kiện (cả `set_var`). |
| `take_item` | `{ "type": "take_item", "item": "crypt_key", "amount": 1 }` | Lấy item khỏi túi (tối đa `amount`, có bao nhiêu lấy bấy nhiêu). Kết hợp `has_item` để kiểm tra trước. |
| `effect` | `{ "type": "effect", "effect": "slowness", "duration": 100, "amplifier": 1, "all": true }` | Thêm hiệu ứng (`duration` tính bằng tick). `"clear": true` → xoá hiệu ứng đó. |
| `particles` | `{ "type": "particles", "particle": "flame", "x": 120.5, "y": 65, "z": -40.5, "count": 30, "spread": 0.5, "speed": 0 }` | Hiện hạt tại vị trí. Hạt cần dữ liệu thêm (dust, block...) chưa hỗ trợ. |
| `countdown` | `{ "type": "countdown", "id": "collapse", "ticks": 1200, "title": "<red>Sụp đổ sau {time}", "color": "red" }` | Hiện thanh boss đếm ngược `ticks` tick cho mọi người trong instance (`{time}` hiển thị theo giây); hết giờ bắn sự kiện `countdown_ended` (subject = `id`). Chạy lại cùng `id` → bắt đầu lại. Không giữ qua restart server. |
| `stop_countdown` | `{ "type": "stop_countdown", "id": "collapse" }` | Huỷ đếm ngược, không bắn sự kiện. |
| `choose` | `{ "type": "choose", "options": [ { "weight": 3, "do": [ ... ] }, { "weight": 1, "do": [ ... ] } ] }` | Chạy ngẫu nhiên 1 lựa chọn theo `weight` (mặc định 1). `do` hoặc `actions`. |
| `command` | `{ "type": "command", "command": "effect give {player} glowing 5", "as_player": false }` | Chạy lệnh từ console (hoặc với tư cách người chơi nếu `as_player`). Hỗ trợ placeholder như `message`. |
| `call` | `{ "type": "call", "function": "open_gate", "args": { "gate": "'crypt'" } }` | Chạy [function](#function). `args` truyền tham số (giá trị có thể là biểu thức). |
| `cancel` | `{ "type": "cancel" }` | Huỷ sự kiện kích hoạt (không đặt được block, không phá được, không vứt được item, ...). Chỉ sự kiện huỷ được, xem [Sự kiện](#sự-kiện). |

| `signal` | `{ "type": "signal", "id": "gate_open", "amount": 1, "data": { "gate": "north", "wave": "v.wave" } }` | Bắn sự kiện tự đặt, giá trị `data` có thể là biểu thức; trigger nhận đọc bằng `c.gate`, `c.wave`. Trigger khác bắt bằng `"on": "gate_open"`; quest nhóm có objective `signal` cùng id cũng tiến `amount`. |
| `start_quest` | `{ "type": "start_quest", "quest": "crypt_epilogue" }` | Giao quest cho nhóm quest của instance, mọi thành viên được nhận thưởng. Cần feature `quests`. |
| `create_region` | `{ "type": "create_region", "id": "arena", "shapes": [ { "from": [100, 60, -50], "to": [120, 80, -30] }, { "from": [120, 60, -40], "to": [140, 70, -35] } ], "priority": 10, "flags": { "pvp": "deny" } }` | Tạo region WorldGuard từ một hoặc nhiều hình hộp (`shapes`); nhiều hình → region nhóm, flag dùng chung. `replace` (mặc định `true`) thay toàn bộ hình cũ; `false` → thêm hình vào region đã có. `priority`, `flags` tuỳ chọn. Flag/giá trị sai → cảnh báo khi nạp. |
| `region_flag` | `{ "type": "region_flag", "region": "arena", "flag": "block-break", "value": "allow", "group": "members" }` | Đặt flag cho region. Bỏ `region` → `__global__`. Bỏ `value` → xoá flag. `group` (`all`, `members`, `owners`, `non_members`, `non_owners`, `none`) mặc định `all`. |
| `remove_region` | `{ "type": "remove_region", "id": "arena" }` | Xoá region (kể cả mọi hình con). |
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
{ "type": "repeat", "times": 3, "interval": 400, "while": "q.mob_count('patrol') < 5", "do": [ ... ] }
```

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `times` | *(bắt buộc)* | Số lần lặp tối đa. |
| `while` | `[]` | [Condition](#condition) (1 cái hoặc danh sách) kiểm tra trước mỗi lần lặp; sai thì dừng. |
| `interval` | `0` | Số tick chờ giữa các lần lặp. `0` = chạy hết ngay trong tick hiện tại. Khi chờ, giống `delay`: dừng nếu instance đã kết thúc. |
| `do` (hoặc `actions`) | *(bắt buộc)* | Action chạy mỗi lần lặp. |

#### `if`

```json
{ "type": "if", "if": "q.has_item('crypt_key')", "then": [ ... ], "else": [ ... ] }
```

Tất cả condition ở `if` (hoặc `conditions`) đúng → chạy `then`, ngược lại chạy `else`. `then` và `else` đều tuỳ chọn.

#### Mob được theo dõi

`spawn_mob` có `tag` vẫn sinh đủ `count` mob mỗi lần chạy; mob được gắn `tag` và không tự biến mất khi ở xa. Plugin **không lưu** gì thêm: condition `mob_count` đếm số mob mang `tag` đang thực sự có trong world, nên mob bị despawn hoặc mất sau restart tự động không còn được đếm. Mob mang `tag` chết vì bất kỳ lý do gì sẽ bắn `mob_died` kèm `tag` — tự đếm bằng biến nếu cần.

Ví dụ: 2 lính gác, mỗi lính 1 `tag` riêng. Vào lại khu vực thì sinh lại lính bị mất (despawn, restart), nhưng lính đã chết thì không bao giờ quay lại:

```json
"features": {
  "triggers": { "triggers": [
    { "on": "mob_died tag=gate_guard_1", "do": "v.guard_1_dead = true" },
    { "on": "mob_died tag=gate_guard_2", "do": "v.guard_2_dead = true" },
    { "on": "region_entered gate_room",
      "do": [
        { "type": "if", "if": "!v.guard_1_dead && q.mob_count('gate_guard_1') == 0",
          "then": { "type": "spawn_mob", "mob": "vein_stalker", "x": 10, "y": 64, "z": 5, "tag": "gate_guard_1" } },
        { "type": "if", "if": "!v.guard_2_dead && q.mob_count('gate_guard_2') == 0",
          "then": { "type": "spawn_mob", "mob": "vein_stalker", "x": 14, "y": 64, "z": 5, "tag": "gate_guard_2" } }
      ] }
  ] }
}
```

Trigger vào khu vực **không** dùng `once`, để mỗi lần vào lại đều kiểm tra và bù lính. Biến chưa gán là `0` (sai), nên không cần khai báo trước.

Objective phòng tính cả khi lính chết không do người chơi:

```json
{ "on": { "event": "mob_died", "data": { "tag": { "in": ["gate_guard_1", "gate_guard_2"] } } }, "count": 2 }
```

Muốn giữ 1 nhóm mob với số lượng cố định (không quan tâm từng con), dùng `repeat` + `mob_count`:

```json
{ "type": "repeat", "times": 5,
  "while": "q.mob_count('patrol') <= 4",
  "do": [ { "type": "spawn_mob", "mob": "minecraft:zombie", "x": 0, "y": 70, "z": 0, "spread": 4, "tag": "patrol" } ] }
```

Text trong `message`, `title`, `countdown` và `command` hỗ trợ:
- `{<biểu thức>}` — giá trị [biểu thức](#biểu-thức), vd. `{v.keys}`, `{p.prayers}/3`, `{v.a + v.b}`, `{q.alive_count}`. Số nguyên không có `.0`; chưa có giá trị → `0`. Cách viết cũ `{var.<tên>}` / `{pvar.<tên>}` vẫn dùng được.
- `{player}` — tên người gây sự kiện.
- `{time}` — chỉ trong `countdown`: thời gian còn lại.

Text có biểu thức sai → dungeon không nạp được.

### Biến

Biến là giá trị **số, chuỗi hoặc true/false** lưu theo từng instance (còn nguyên sau khi restart server). Dùng để đếm, đánh dấu tiến độ câu đố, nhớ lựa chọn của người chơi.

- Khai báo giá trị ban đầu ở `variables` của file dungeon. Instance cũ nhận thêm biến mới khai báo khi được nạp lại.
- Ghi: lệnh gán (`"v.x = 1"`), `set_var`, `add_var`, và `once` của trigger.
- Đọc: `v.<tên>` trong biểu thức, condition `var`, `{v.<tên>}` trong text.
- **Biến riêng người chơi**: `p.<tên>` trong biểu thức, hoặc thêm `"player": true` vào `set_var` / `add_var` / condition `var`. Mỗi người có giá trị riêng (lưu dưới tên `player.<uuid>.<tên>`). Không có người gây sự kiện → dùng biến chung.

### Function

Danh sách action dùng lại nhiều nơi, khai báo ở `functions` của file dungeon và gọi bằng action `call`:
```json
"functions": {
  "open_gate": [
    { "type": "fill_block", "from": [118, 64, -42], "to": [122, 67, -42], "block": "minecraft:air" },
    { "type": "play_sound", "sound": "minecraft:block.iron_door.open", "all": true }
  ]
}
```
Function không có tham số nhận người gây sự kiện và dữ liệu `c.` của nơi gọi.

**Có tham số**: dạng object với `params` và `do`. Bên trong, `c.<tham số>` là giá trị truyền qua `args`; dữ liệu sự kiện của nơi gọi **không** được truyền vào (người gây sự kiện thì vẫn có).
```json
"functions": {
  "reward": { "params": [ "item", "amount" ], "do": [
    { "type": "message", "text": "<gold>Nhận {c.amount} phần thưởng" },
    "v.rewards += c.amount" ] }
}
```
```json
{ "type": "call", "function": "reward", "args": { "item": "'crypt_relic'", "amount": "v.wave * 2" } }
```
Gọi function không tồn tại, thiếu / thừa tham số, hoặc function tự gọi lại chính nó (trực tiếp hay qua function khác) → cảnh báo khi nạp; khi chạy, lồng quá 16 tầng thì dừng.

### Ví dụ đầy đủ

Các ví dụ dưới đây là phần `triggers` (và `functions` / `variables` nếu cần) của 1 file dungeon. Toạ độ chỉ để minh hoạ.

#### Cửa khoá bằng chìa

Người chơi cầm `crypt_key` chuột phải vào cửa sắt thì cửa mở và chìa bị lấy đi; không có chìa thì nhận thông báo. `cooldown` chặn spam tin nhắn khi bấm liên tục. Hành động mở cửa đặt trong function để tái sử dụng (vd. lệnh admin, hoặc mở cửa khi boss chết).

```json
"variables": { "gate_open": false },
"functions": {
  "open_crypt_gate": [
    { "type": "fill_block", "from": [118, 64, -42], "to": [122, 67, -42], "block": "minecraft:air", "replace": "minecraft:iron_bars" },
    { "type": "particles", "particle": "minecraft:cloud", "x": 120.5, "y": 65.5, "z": -41.5, "count": 40, "spread": 1.5 },
    { "type": "play_sound", "sound": "minecraft:block.iron_door.open", "all": true },
    "v.gate_open = true"
  ]
},
"features": {
  "triggers": { "triggers": [
    { "on": "block_interacted minecraft:iron_bars in crypt_gate", "cooldown": 40, "if": "!v.gate_open",
      "do": [
        { "type": "if", "if": "q.has_item('crypt_key')",
          "then": [
            { "type": "take_item", "item": "crypt_key" },
            { "type": "message", "text": "<green>{player} đã mở cổng hầm mộ.", "all": true },
            { "type": "call", "function": "open_crypt_gate" } ],
          "else": [ { "type": "title", "subtitle": "<gray>Cần <gold>Chìa hầm mộ</gold> để mở" } ] } ] }
  ] }
}
```

- `in crypt_gate` ở matcher: chỉ cửa sắt **trong** region này mới tính, các song sắt khác trong map không kích hoạt.
- `has_item` / `take_item` dùng id trần cho item tuỳ chỉnh; item vanilla viết `minecraft:...`.
- `fill_block` với `replace` chỉ xoá song sắt, tường đá xung quanh trong khối hộp giữ nguyên.

#### Câu đố đặt khối lên bệ thờ

Đặt đủ 4 khối vàng lên bệ (region `altar`). Mỗi lần đặt cộng biến, phá khối trên bệ thì trừ lại; trigger `when` mở thưởng đúng lúc biến chạm 4.

```json
"variables": { "altar_gold": 0 },
"features": {
  "triggers": { "triggers": [
    { "on": "block_placed minecraft:gold_block in altar",
      "do": [
        "v.altar_gold += 1",
        { "type": "particles", "particle": "minecraft:end_rod", "x": 200.5, "y": 66, "z": 40.5, "count": 15, "spread": 0.3 },
        { "type": "message", "text": "<yellow>Bệ thờ: {v.altar_gold}/4", "all": true } ] },
    { "on": "block_broken minecraft:gold_block in altar", "do": "v.altar_gold -= 1" },
    { "when": "v.altar_gold >= 4", "once": true,
      "do": [
        { "type": "title", "title": "<gold>Bệ thờ thức tỉnh", "all": true },
        { "type": "set_block", "x": 200, "y": 65, "z": 44, "block": "ancient_reliquary" },
        { "type": "give_item", "item": "altar_blessing", "all": true } ] }
  ] }
}
```

- `set_block` đặt block **tuỳ chỉnh** `ancient_reliquary` (id trần) — block vanilla thì viết `minecraft:...`.
- `once` ở trigger cuối: phá rồi đặt lại khối thứ 4 không trao thưởng lần nữa.

#### Câu đố 3 cần gạt

Cả 3 cần gạt phải ở trạng thái bật cùng lúc. `c.powered` cho biết cần gạt vừa bật hay tắt, nên mỗi cần gạt chỉ cần 1 trigger chép trạng thái đó vào biến; trigger `when` mở cửa khi cả 3 biến cùng đúng.

```json
"variables": { "lever_a": false, "lever_b": false, "lever_c": false },
"features": {
  "triggers": { "triggers": [
    { "on": "redstone_activated @ 50 64 10", "do": "v.lever_a = c.powered" },
    { "on": "redstone_activated @ 54 64 10", "do": "v.lever_b = c.powered" },
    { "on": "redstone_activated @ 58 64 10", "do": "v.lever_c = c.powered" },
    { "when": "v.lever_a && v.lever_b && v.lever_c", "once": true,
      "do": { "type": "fill_block", "from": [52, 64, 14], "to": [56, 66, 14], "block": "minecraft:air" } }
  ] }
}
```

Tấm áp lực và nút bấm luôn có `powered: true`; dùng `"v.plates += 1"` để đếm thay vì chép trạng thái.

#### Đấu trường boss: nhốt người chơi và đếm ngược

Khi cả đội vào đấu trường: tạo region nhiều hình (sàn chính + ban công) chặn rời khỏi và cấm phá block, rồi bắt đầu đếm ngược 3 phút. Trong lúc đánh, cứ 10 giây boss phun lửa linh hồn làm chậm cả đội. Hết giờ thì thất bại; boss chết trước thì dừng đếm ngược và gỡ region.

```json
"features": {
  "triggers": { "triggers": [
    { "on": "region_entered arena_door", "once": true, "if": "q.in_region('arena_door', 'all')",
      "do": [
        { "type": "create_region", "id": "arena_lock",
          "shapes": [
            { "from": [300, 60, 300], "to": [340, 85, 340] },
            { "from": [340, 70, 310], "to": [348, 78, 330] } ],
          "priority": 20,
          "flags": { "exit": "deny", "block-break": "deny" } },
        { "type": "countdown", "id": "boss_enrage", "ticks": 3600, "title": "<red>Boss nổi điên sau <white>{time}", "color": "red" },
        { "type": "spawn_mob", "mob": "hellknight", "x": 320, "y": 62, "z": 320 },
        "v.fight_active = true" ] },

    { "on": "timer", "interval": 200, "if": "v.fight_active",
      "do": [
        { "type": "particles", "particle": "minecraft:soul_fire_flame", "x": 320.5, "y": 61, "z": 320.5, "count": 80, "spread": 4 },
        { "type": "effect", "effect": "minecraft:slowness", "duration": 60, "amplifier": 1, "all": true } ] },

    { "on": "countdown_ended boss_enrage",
      "do": [
        { "type": "title", "title": "<dark_red>Thất bại", "subtitle": "<gray>Boss đã nổi điên", "all": true },
        { "type": "end", "reason": "hết thời gian đánh boss" } ] },

    { "on": "mob_killed hellknight", "once": true,
      "do": [
        "v.fight_active = false",
        { "type": "stop_countdown", "id": "boss_enrage" },
        { "type": "remove_region", "id": "arena_lock" },
        { "type": "effect", "effect": "minecraft:regeneration", "duration": 200, "amplifier": 1, "all": true } ] },

    { "on": "player_died", "if": "q.alive_count == 0", "do": { "type": "end", "reason": "cả đội đã gục" } }
  ] }
}
```

- `q.in_region('arena_door', 'all')`: chỉ bắt đầu khi **cả đội** đã đứng ở cửa, không ai bị nhốt ngoài.
- `create_region` nhiều `shapes` gộp thành 1 region, flag áp dụng chung cho cả hai hình.
- `interval` tính bằng tick (200 = 10 giây); biến `fight_active` tắt timer khi trận kết thúc.
- `countdown` hiện thanh boss cho mọi người trong instance; `{time}` hiển thị theo giây.

#### Đền ban phước theo từng người

Mỗi người chỉ được cầu nguyện 3 lần. Biến `p.` đếm riêng từng người; `cooldown` chống bấm liên tục; `choose` chọn ngẫu nhiên phần thưởng, `math.random` thêm cơ hội hiếm.

```json
{ "on": "block_interacted minecraft:lodestone in shrine", "cooldown": 20,
  "do": [
    { "type": "if", "if": "p.prayers >= 3",
      "then": [ { "type": "message", "text": "<gray>Đền thờ không đáp lại bạn nữa." } ],
      "else": [
        "p.prayers += 1",
        { "type": "message", "text": "<aqua>Lời cầu nguyện thứ {p.prayers}/3..." },
        { "type": "choose", "options": [
          { "weight": 3, "do": [ { "type": "effect", "effect": "minecraft:regeneration", "duration": 200, "amplifier": 1 } ] },
          { "weight": 2, "do": [ { "type": "effect", "effect": "minecraft:strength", "duration": 600 } ] },
          { "weight": 1, "do": [ { "type": "give_item", "item": "minecraft:golden_apple" } ] } ] },
        { "type": "if", "if": "math.random(0, 1) < 0.05 && q.player_health <= 50",
          "then": [
            { "type": "give_item", "item": "shrine_relic" },
            { "type": "message", "text": "<gold>{player} nhận được thánh tích!", "all": true } ] } ] }
  ] }
```

- Biến chưa có giá trị là `0`, nên lần đầu `p.prayers >= 3` sai và luôn vào nhánh `else`.
- `math.random(0, 1) < 0.05 && q.player_health <= 50`: chỉ người đang dưới 50% máu mới có 5% cơ hội nhận thánh tích.

## Kiểm tra khi nạp

Sau mỗi lần nạp (khởi động, hoặc `/smpreload dungeon/definitions` / `dungeon/rooms`), mọi dungeon được kiểm tra và sự kiện Bukkit chỉ dành cho script được đăng ký lại theo những gì dungeon đang dùng. Lỗi **chỉ log cảnh báo**, dungeon vẫn nạp (riêng lỗi cú pháp biểu thức làm dungeon không nạp được). Mỗi dòng có đường dẫn tới chỗ sai:

```
Dungeon crypt: features.triggers.triggers[3].on: event 'gate_opn' is never fired by this dungeon
Dungeon crypt: features.triggers.triggers[1].actions[1]: variable 'kyes' is never declared or set
Dungeon crypt: features.rooms.room 'crypt_boss'.objectives[0].on: event 'mob_damaged' never carries data 'hp'
Dungeon crypt: features.triggers.triggers[0].do[0]: c.powerd is not provided by event 'redstone_activated' (has: x, y, z, powered)
Dungeon crypt: features.reward_chests.loot_table: unknown loot table 'crypt_chst'
```

Được kiểm tra:
- Sự kiện mà matcher nghe nhưng không có gì bắn (không phải built-in, không có `signal` nào trong dungeon gửi).
- Khoá `data` mà sự kiện không bao giờ mang.
- Biến được đọc nhưng chưa khai báo và không có action nào gán (kể cả biến đọc trong biểu thức, text).
- `c.<khoá>` mà sự kiện của trigger không mang; `c.` trong trigger `when`.
- `p.` và query cần người chơi ở chỗ không có người (`when`, `timer`, `started`, `countdown_ended`, `quest_completed`).
- `cancel` trên sự kiện không huỷ được.
- `call` thiếu / thừa tham số.
- Chuỗi giá trị trông như biểu thức nhưng sai cú pháp (bị dùng như chữ).
- Trigger `when` dùng query hoặc không đọc biến `v.` nào mà không có `recheck`.
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
