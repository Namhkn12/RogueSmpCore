# JSON Reference — Dungeons

Dungeon là hệ **không** dùng Registry/Codec: nó đọc JSON bằng Gson + repository riêng, nên quy tắc khác phần còn lại (đọc trước phần "Quy tắc nạp file"). Thư mục gốc: `dungeon_v2/template/`.

| Thư mục | Chứa | Cách tạo |
| :--- | :--- | :--- |
| `dungeon_template/` | Dungeon (`Dungeon`) | viết tay |
| `room_template/` | Phòng (`Room`) | viết tay |
| `spawner_template/` | Spawner (`Spawner`) | viết tay |
| `schemeta/` | Metadata schematic (`Schemeta`) | sinh bởi `/schemeta save <tên>` (sửa tay được) |
| `schematic/<schemetaId>.schem` | Kiến trúc phòng (Sponge schematic) | sinh bởi `/schemeta save <tên>` |

`dungeon_v2/runtime/**` (region, party, session) là dữ liệu runtime tự sinh — không sửa. Thế giới dungeon chạy trong world tên `dungeon_<uuid>` ở thư mục gốc server.

## Quy tắc nạp file (khác Registry!)

- Đọc mọi `*.json` **trực tiếp** trong thư mục (**không đệ quy**).
- **Id lấy từ field `"id"` trong file, không phải tên file.** Tên file không ý nghĩa khi nạp; 2 file trùng `id` → file được liệt kê sau thắng. File thiếu `id` bị bỏ (dungeon: im lặng; room: log lỗi; spawner: chỉ log debug).
- Tên key = **tên field Java đúng nguyên văn** (camelCase: `lootTableId`, `schemetaId`, `minDelay`...). Key lạ bị bỏ qua.
- Thiếu key → giữ giá trị mặc định của constructor (thường `null`/`0`/`false`).
- **Enum phân biệt hoa/thường** và chuỗi enum sai trở thành `null` **im lặng** (`"normal"` ≠ `"NORMAL"`).
- Số trong `params` luôn là số thập phân khi đọc, nhưng code chấp nhận mọi số; **kiểu sai trong `params` âm thầm rơi về mặc định**.
- Số nguyên thật (vd. `Spawner.mobs`) mà ghi `1.5` → fail parse, **file bị từ chối**.
- Lệnh admin (chỉ trong world có tên bắt đầu `building`): `/schemeta save <tên>` (tạo schemeta id `schemeta_<tên>`), `/schemeta paste <id>`, `/template spawner get <id>` (lấy item đặt spawner), `/template loottable roll|check|treasure give`, `/template reload [all|spawner|dungeon|room|loottable|schemeta]`.

## `Dungeon`

```json
{
  "id": "crypt",
  "name": "Hầm mộ",
  "description": "...",
  "lootTableId": "dungeons/crypt_reward",
  "playTime": 30,
  "active": true,
  "minimumRooms": 4,
  "pools": [ ... ],
  "treasureRooms": ["crypt_treasure"],
  "bosses": ["bosses/crypt_lord"]
}
```

| Key | Kiểu | Ý nghĩa thực tế |
| :--- | :--- | :--- |
| `id` | string | Id dungeon (dùng khi bắt đầu dungeon bằng lệnh `dungeon`). |
| `name` | string | Hiện trên màn hình vào dungeon và scoreboard. |
| `description` | string | **Chưa dùng.** |
| `lootTableId` | string | Id loot table trong `loottable/`. Chỉ dùng cho rương kho báu có khóa thưởng `"dungeon"` (tạo bằng `/template loottable treasure give dungeon`): rương được roll theo bảng này; rương đôi roll 2 lần; bảng không tồn tại → báo lỗi cho người chơi. Rương khác dùng bảng riêng của chúng. |
| `playTime` | int (**phút**) | Thời gian tối đa của 1 lượt. Hết giờ → lượt thất bại, cả nhóm bị đưa về world `building`. |
| `active` | boolean | **Chưa dùng** (không có chỗ nào kiểm tra). |
| `minimumRooms` | int | Phải dọn xong **ít nhất** từng này phòng thì phòng `BOSS` mới được đưa vào lựa chọn cửa. |
| `pools` | mảng pool | Cách chọn phòng cho 1 lượt (xem dưới). |
| `treasureRooms` | mảng id phòng | Mỗi lượt chọn **ngẫu nhiên 1** phòng; sau khi boss chết và vào cổng kho báu, phòng đó được dán + dịch chuyển tới. **Phải có ít nhất 1 phần tử** (thiếu/rỗng gây lỗi). Không đi qua `pools`. |
| `bosses` | mảng string | **Chưa dùng** — boss thực sự lấy từ `demon_slayer.target` của phòng BOSS. |

### `pools` — chọn phòng cho 1 lượt

Khi tạo lượt chơi, **mỗi pool** được roll: số phòng = `min + ngẫu nhiên(0..max−min)` (giới hạn bởi số phòng còn lại; `max < min` gây lỗi; `min = max = 0` = không lấy phòng), chọn **không hoàn lại theo trọng số** từ `rooms`. 1 phòng đã được pool trước chọn thì không được chọn lại → mỗi `roomId` xuất hiện tối đa 1 lần/lượt. Tổng trọng số = 0 → không chọn được gì. Kết quả gộp thành "kho phòng" của lượt chơi:
- **Phòng đầu tiên** = phòng `SPAWN` đầu tiên trong kho — vì vậy phải có pool luôn đảm bảo ra 1 phòng `SPAWN`, nếu không dungeon không bắt đầu.
- Mỗi cửa tiếp theo đưa ra **1–4 phòng ngẫu nhiên** từ kho còn lại, **không bao giờ** là `SPAWN`/`TREASURE` hay phòng hiện tại; phòng `BOSS` chỉ xuất hiện sau khi đã dọn ≥ `minimumRooms` phòng. Phòng đã dọn bị loại khỏi kho.

| Key (`RoomPool`) | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `min`, `max` | int | Số phòng lấy từ pool này (xem trên). |
| `rooms` | mảng `{roomId, weight}` | Ứng viên. `roomId` = `id` của phòng trong `room_template/`; `weight` (double, trọng số). |
| `id`, `icon`, `name`, `weight` | — | **Chưa dùng** khi roll (có thể để làm ghi chú). |

## `Room`

```json
{
  "id": "crypt_a",
  "type": "NORMAL",
  "schemetaId": "schemeta_crypt_a",
  "objectives": [ { "type": "monster_hunter", "params": { "require": 8, "target": "crypt/skeleton", "score": 10 } } ],
  "roomEvents": [ { "type": "dark_eyes", "params": { "isDisrupt": true } } ]
}
```

| Key | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `id` | string | Id phòng; cũng hiện làm tên phòng ở GUI chọn cửa. |
| `type` | enum `RoomType` | Loại phòng (bảng dưới). Sai chính tả → `null` im lặng. |
| `schemetaId` | string | `id` của file `schemeta/` (vd. `"schemeta_crypt_a"`); schematic được tìm ở `schematic/<id>.schem`. |
| `objectives` | mảng `{type, params}` | Mục tiêu dọn phòng. Phòng xong khi **mọi** objective xong; **phòng không có objective hoàn thành ngay**. |
| `roomEvents` | mảng `{type, params}` | Sự kiện trong phòng. |

`RoomType` (đúng chữ hoa): `SPAWN`, `WARMUP`, `NORMAL`, `ELITE`, `CHECKPOINT`, `SAFE`, `TRADE`, `PUZZLE`, `BOSS`, `TREASURE`, `TRAP`, `LOOT`, `EVENT`. Loại **chiến đấu**: `NORMAL`, `ELITE`, `BOSS` — cửa phòng chiến đấu đóng lại 5 giây sau khi vào, người chưa vào kịp bị dịch chuyển tới cửa. Icon trên GUI chọn cửa: SPAWN `GRASS_BLOCK`, WARMUP `WOODEN_SWORD`, NORMAL `STONE`, ELITE `DIAMOND_SWORD`, CHECKPOINT `BEACON`, SAFE `BED`, TRADE `EMERALD`, PUZZLE `COMPARATOR`, BOSS `NETHER_STAR`, TREASURE `ENDER_CHEST`, TRAP `TNT`, LOOT `CHEST`, EVENT `FIREWORK_ROCKET`.

## Objective — `{ "type": ..., "params": { ... } }`

**Tham số nằm trong `params`** (không phẳng cạnh `type`; các ví dụ phẳng trong javadoc cũ của code là sai). Loại lạ → ném lỗi. Mọi objective đọc thêm `score` (int, mặc định 0): điểm cộng vào điểm dungeon khi objective xong.

| `type` | `params` | Cách tính tiến độ |
| :--- | :--- | :--- |
| `spawner_breaker` | `require` (int, `3`; chấp nhận alias `required`), `score` | +1 mỗi **spawner vanilla** bị phá trong phạm vi phòng. |
| `monster_hunter` | `require` (int, `10`; alias `required`), `target` (string, mặc định = **bất kỳ mob**), `score` | +1 mỗi mob khớp bị giết. |
| `item_collector` | `require` (int, `1`; **không** chấp nhận `required`), `target` (string, mặc định = bất kỳ item), `score` | Cộng số lượng item nhặt, tối đa `require`. |
| `demon_slayer` | `require` (int, `1`; alias `required`), `target`, `score` | Như `monster_hunter` nhưng phạm vi **toàn dungeon**: xong → đánh dấu boss đã bị hạ, đồng hồ đặt lại còn 5 phút. |

`target` khớp với: mob → id entity trong `entities/` (nếu mob không có id plugin, rơi về tên loại vanilla chữ thường, vd. `"zombie"`); item → id item trong `items/` (nếu không có, rơi về tên Material chữ thường). Chỉ sự kiện xảy ra **trong phạm vi phòng hiện tại** và phòng chưa xong mới được tính.

**Phòng `BOSS`**: boss được spawn từ `target` của `demon_slayer` **đầu tiên** của phòng (id entity trong `entities/`) tại block kích hoạt; thiếu `target` thì dùng chuỗi chữ `"fallback here"` (sẽ không ra gì) — luôn khai `target` cho phòng boss.

## Room event — `{ "type": ..., "params": { ... } }`

Loại lạ → ném lỗi. Param kiểu sai → giữ mặc định. Sự kiện được gọi **mỗi giây** (bộ đếm 20 tick), nên "interval" bên dưới thực chất tính theo giây.

| `type` | `params` | Tác dụng |
| :--- | :--- | :--- |
| `noop` | — | Không làm gì. |
| `party_strength_buff` | `durationTicks` (int, `3600`), `amplifier` (int, `1`) | Khi bắt đầu phòng, tặng mỗi thành viên nhóm online hiệu ứng `damage_increase` (`durationTicks`, `amplifier`). |
| `dark_eyes` | `isDisrupt` (boolean, `false`) | Cảnh báo "The darkness is watching you...", liên tục làm mù (Blindness 40 tick). `isDisrupt: true` → chỉ mù theo nhịp (có khoảng nhìn được ngắn). Hết phòng gỡ mù. |
| `creeping_dread` | `maxTriggers` (int, `4`), `spawnChance` (double, `0.25`), `minInterval` (int, `40`), `maxInterval` (int, `100`) | Mỗi người chơi, tối đa `maxTriggers` lần: phát tiếng creeper châm ngòi gần họ, rồi với xác suất `spawnChance` spawn 1 Creeper vanilla cách sau lưng 3 block. Lần kế tiếp sau `minInterval`–`maxInterval` (đơn vị bộ đếm). |

## `Spawner`

```json
{
  "id": "skel_nest",
  "name": "Ổ xương",
  "mobs": { "crypt/skeleton": 3, "crypt/archer": 1 },
  "delay": 20, "minDelay": 100, "maxDelay": 200,
  "activeRange": 16, "spawnRange": 4,
  "maxNearBy": 6, "spawnCount": 2,
  "behaviors": [ { "type": "wave", "params": { "waves": 3 } } ]
}
```

Spawner được áp lên 1 **spawner vanilla** khi đặt (item lấy bằng `/template spawner get <id>`). Sau đó cơ chế spawn vanilla chạy theo các số dưới.

| Key | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `id` | string | Id spawner. |
| `name` | string | Tên item spawner (rơi về `id` nếu thiếu). |
| `mobs` | object: **id entity → trọng số (int)** | **Bắt buộc** (thiếu → NPE). Key = id trong `entities/`; value là **trọng số**, không phải số lượng; phải là số nguyên. Id entity không tồn tại bị bỏ qua. |
| `delay` | int tick | Bộ đếm hiện tại — lần spawn đầu sau chừng này tick. |
| `minDelay`, `maxDelay` | int tick | Khoảng chờ giữa các lần spawn (ngẫu nhiên mỗi chu kỳ). |
| `activeRange` | int block | Người chơi phải ở trong bán kính này spawner mới chạy. |
| `spawnRange` | int block | Bán kính spawn quanh block. |
| `maxNearBy` | int | Không spawn nếu đã có chừng này mob gần đó. |
| `spawnCount` | int | Số mob thử spawn mỗi chu kỳ. |
| `behaviors` | mảng `{type, params}` | **Bắt buộc có key** (thiếu → NPE); không muốn hành vi nào thì dùng `[]`. |

### Behavior — `{ "type": ..., "params": { ... } }`

Loại lạ → ném lỗi. Nhiều behavior trên 1 spawner được kết hợp: 1 lần phá/spawn bị **hủy nếu BẤT KỲ** behavior nào từ chối.

| `type` | `params` | Tác dụng |
| :--- | :--- | :--- |
| `protector` | `requireBreak` (int, `3`) | Phải phá đủ N lần; lần thứ N mới thật sự phá được. Các lần trước bị hủy kèm báo "còn N lần". |
| `wave` | `waves` (int, `3`) | Mỗi lần spawn tính 1 wave; **chặn phá** cho tới khi đủ `waves` lần spawn. Mỗi lần spawn nhân thêm `số người chơi trong 20 block + 2` bản mob ở vị trí ngẫu nhiên gần đó. |
| `teleport` | `max_teleports` (int, `3`), `teleport_radius` (double, `20.0`), `safe_check_radius` (double, `3.0`) — **snake_case**, khác các loại kia | Mỗi lần cố phá, spawner dịch tới vị trí an toàn ngẫu nhiên cách 10–`teleport_radius` block, đến khi hết `max_teleports` thì cho phá. Khi spawn: hủy spawn vanilla, thay bằng 2–4 mob gần mỗi người chơi (1.5–`safe_check_radius` block). |
| `cursed` | — | Mỗi lần spawn, mỗi người chơi gần đó nhận 2 lời nguyền ngẫu nhiên khác nhau: buồn nôn 15s, mù 4s, độc 15s, mạng nhện dưới chân (nếu là không khí), cháy 4s. |

"Người chơi gần" = trong 20 block quanh spawner.

## Ví dụ

```json
// dungeon_template/crypt.json
{
  "id": "crypt", "name": "Hầm mộ", "lootTableId": "dungeons/crypt_reward",
  "playTime": 30, "active": true, "minimumRooms": 4,
  "pools": [
    { "id": "spawn", "min": 1, "max": 1, "rooms": [ { "roomId": "crypt_spawn", "weight": 1 } ] },
    { "id": "combat", "min": 4, "max": 6,
      "rooms": [ { "roomId": "crypt_a", "weight": 3 }, { "roomId": "crypt_b", "weight": 1 } ] },
    { "id": "boss", "min": 1, "max": 1, "rooms": [ { "roomId": "crypt_boss", "weight": 1 } ] }
  ],
  "treasureRooms": ["crypt_treasure"],
  "bosses": ["bosses/crypt_lord"]
}
```
```json
// room_template/room_crypt_boss.json
{
  "id": "crypt_boss", "type": "BOSS", "schemetaId": "schemeta_crypt_boss",
  "objectives": [ { "type": "demon_slayer", "params": { "target": "bosses/crypt_lord", "score": 100 } } ],
  "roomEvents": []
}
```

## Trường tồn tại nhưng chưa có tác dụng

`Dungeon.active`, `Dungeon.description`, `Dungeon.bosses`, `RoomPool.id/icon/name/weight`, `Schemeta.schematic` — được đọc nhưng không ảnh hưởng gì khi chạy (an toàn để dùng làm ghi chú).

## Dành cho dev

Kiến trúc 7 tầng, bootstrap và luồng chạy: [Dungeon System](Dungeon-System.md). Loại objective/room-event/behavior được tạo bằng `switch` trong `ObjectiveFactory`/`RoomEventFactory`/`BehaviorFactory` (đọc `Map<String,Object> params` lỏng lẻo, không phải registry kiểu Codec). Thêm loại mới = thêm class + 1 nhánh `case` trong factory tương ứng.

---
◀ [JSON-Blocks](JSON-Blocks.md) · Về [Trang chủ](Home.md)
