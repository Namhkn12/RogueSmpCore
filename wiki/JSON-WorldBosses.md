# JSON Reference — World Boss

Thư mục: `world_bosses/` (id = đường dẫn bỏ `.json`, **không có** field `"id"`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

World boss là trận đánh boss công khai tại 1 địa điểm cố định trên server. File JSON chỉ chứa **vị trí, thông số và cổng triệu hồi**; mọi thứ thuộc về "tính cách" của boss (tin nhắn, title, âm thanh, hiệu ứng, mob nào được spawn, khi nào phát thưởng) nằm trong **class encounter viết bằng code** — xem [World Boss System](World-Boss-System.md).

⚠️ **Id file phải trùng id encounter** đã đăng ký trong [`WorldBossEncounters`](../src/main/java/com/roguesmp/worldboss/encounter/WorldBossEncounters.java). `world_bosses/goblin_king.json` dùng encounter `"goblin_king"`. Không có encounter tương ứng → log lỗi lúc khởi động và boss không mở được (`UNKNOWN_ENCOUNTER`). Vì vậy nên để file ở thẳng `world_bosses/`, không đặt trong thư mục con.

## Cấu trúc file

`world_bosses/goblin_king.json`:

```json
{
  "display_name": "<gold>Vua Yêu Tinh",
  "description": [
    "Kẻ thống trị lũ yêu tinh, tham lam và tàn bạo.",
    "Hắn chỉ xuất hiện khi có kẻ dâng đủ vàng."
  ],
  "icon": "GOLDEN_HELMET",
  "location": { "world": "minecraft:overworld", "x": 120.5, "y": 64, "z": -340.5 },
  "arena": "goblin_king_arena",
  "exit": { "type": "region", "region": "goblin_village" },
  "min_participants": 1,
  "requirements": [
    { "type": "adventure_level", "min": 10 }
  ],
  "spawn": {
    "type": "summoned",
    "join_window_seconds": 60,
    "cooldown_seconds": 3600,
    "costs": [
      { "type": "item", "item": "minecraft:gold_block", "amount": 8 },
      { "type": "money", "amount": 5000 }
    ]
  },
  "fight_timeout_seconds": 900,
  "loot_table": "world_boss/goblin_king"
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `display_name` | string MiniMessage | **bắt buộc** | Tên boss. Dùng cho tiêu đề GUI, thanh action bar và placeholder `<boss>` trong tin nhắn. |
| `description` | mảng string | `[]` | Các dòng mô tả trong GUI triệu hồi (tự thêm màu xám). |
| `icon` | tên material | `NETHER_STAR` | Icon thông tin trong GUI. Tên material Bukkit (`GOLDEN_HELMET`, `golden_helmet` đều được). |
| `location` | [WorldPos](#vị-trí-location) | **bắt buộc** | Điểm spawn boss. Phải nằm trong `arena`. |
| `arena` | id region | **bắt buộc** | Region làm đấu trường, nằm ở world của `location`. Region nhiều mảnh vẫn dùng được (tính mọi mảnh). |
| `exit` | [điểm đến](JSON-Warps.md#điểm-đến-destination) | `{ "type": "world_spawn" }` | Nơi người chơi bị đưa tới khi bị loại hoặc bị đuổi khỏi trận. Xem [Lối ra](#lối-ra-exit). |
| `min_participants` | int | `1` | Lúc đấu trường đóng, ít hơn số này → trận bị huỷ. |
| `requirements` | mảng requirement | `[]` | Điều kiện để **triệu hồi** và để **tham gia**. Cùng kiểu với requirement của [cửa hàng](JSON-Trades.md). Xem [Điều kiện](#điều-kiện-requirements). |
| `spawn` | [kiểu xuất hiện](#kiểu-xuất-hiện-spawn) | **bắt buộc** | Boss do người chơi triệu hồi hay tự xuất hiện theo chu kỳ. |
| `fight_timeout_seconds` | int | **bắt buộc** | Giới hạn thời gian trận đấu, tính từ lúc đấu trường đóng. Hết giờ → boss biến mất, không ai nhận thưởng. |
| `loot_table` | id loot table | **bắt buộc** | Bảng loot, roll **riêng cho từng người sống sót**. Xem [JSON-Loot](JSON-Loot.md). |

## Vị trí (`location`)

```json
{ "world": "minecraft:overworld", "x": 120.5, "y": 64, "z": -340.5, "yaw": 0, "pitch": 0 }
```

`world` là **key đầy đủ có namespace** (`minecraft:overworld`), không phải tên thư mục. World Multiverse có key dạng `minecraft:<tên world>`. `yaw`/`pitch` không bắt buộc (mặc định `0`). World chưa nạp lúc mở trận → trận không mở (`WORLD_NOT_LOADED`), GUI báo "Không khả dụng".

## Kiểu xuất hiện (`spawn`)

Field chọn loại: `"type"`.

### `summoned` — người chơi triệu hồi

```json
"spawn": {
  "type": "summoned",
  "join_window_seconds": 60,
  "cooldown_seconds": 3600,
  "costs": [ { "type": "item", "item": "minecraft:gold_block", "amount": 8 } ]
}
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `join_window_seconds` | int | **bắt buộc** | Thời gian tập hợp sau khi triệu hồi, trước khi đấu trường đóng. |
| `cooldown_seconds` | int | `0` | Thời gian chờ trước khi triệu hồi lại được. Bắt đầu tính **lúc đấu trường đóng**, không phải lúc triệu hồi. |
| `costs` | mảng cost | `[]` | Chi phí triệu hồi (`item`, `money`, `exp`). Cùng kiểu với cost của [cửa hàng](JSON-Trades.md). |

Triệu hồi qua GUI — mở bằng action `open_gui` với id `world_boss:<id boss>`:

```json
{ "type": "open_gui", "gui": "world_boss:goblin_king" }
```

Chi phí bị trừ **ngay khi trận mở thành công** và **không hoàn lại**, kể cả khi trận bị huỷ (thiếu người, server tắt, admin huỷ).

### `periodic` — tự xuất hiện theo chu kỳ

```json
"spawn": {
  "type": "periodic",
  "join_window_seconds": 120,
  "interval_seconds": 7200,
  "jitter_seconds": 600,
  "min_online_players": 3
}
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `join_window_seconds` | int | **bắt buộc** | Thời gian tập hợp trước khi đấu trường đóng. |
| `interval_seconds` | int | **bắt buộc** | Khoảng cách giữa 2 lần xuất hiện. |
| `jitter_seconds` | int | `0` | Lệch ngẫu nhiên ±giá trị này mỗi lần, để giờ xuất hiện khó đoán. |
| `min_online_players` | int | `0` | Đến giờ mà server ít người hơn số này → chờ, thử lại mỗi 20 giây. |

Lần xuất hiện kế tiếp được hẹn **lúc đấu trường đóng**: `bây giờ + interval ± jitter`. Lịch được kiểm mỗi 20 giây.

⚠️ Cooldown và lịch xuất hiện **chỉ nằm trong bộ nhớ**, không lưu ra file. Khởi động lại server = cooldown triệu hồi bị xoá, boss chu kỳ bắt đầu đếm lại 1 `interval` mới.

## Điều kiện (`requirements`)

Áp dụng cho **mọi người**, cả 2 kiểu xuất hiện:

- **Người triệu hồi**: GUI kiểm trước khi cho bấm triệu hồi.
- **Người tham gia**: lúc đấu trường đóng, ai đứng trong đấu trường mà không đạt điều kiện sẽ bị đưa tới `exit` và không được tính là người tham gia.

Tin nhắn của encounter có thể kèm **hover liệt kê điều kiện** (✔/✘ theo từng người đọc) — xem [World Boss System](World-Boss-System.md#announcer).

## Lối ra (`exit`)

Người chơi được đưa tới `exit` khi:

| Tình huống | Cách đưa |
| :--- | :--- |
| Không đạt điều kiện lúc đấu trường đóng | Dịch chuyển ngay. |
| Chết trong trận | Hồi sinh tại `exit` (thay cho giường / điểm hồi sinh thường). |
| Cố đi bộ vào đấu trường đang đóng | Dịch chuyển tới `exit`. |
| Vào lại server khi đang đứng trong đấu trường đang đóng | Dịch chuyển ngay khi vào. |

Dịch chuyển vào đấu trường đang đóng (lệnh, ender pearl...) bị chặn, người chơi đứng yên. Người tự đi ra khỏi đấu trường bị loại nhưng **không** bị dịch chuyển (vì đã ở ngoài). Không tra được `exit` (world chưa nạp, region không có) → dùng spawn của world người chơi. Dịch chuyển là tức thì, không có thời gian chờ hay hiệu ứng của `/warp`.

## Diễn biến trận đấu

1. **Mở** (triệu hồi hoặc đến giờ): bắt đầu đếm ngược `join_window_seconds`. Người trong đấu trường thấy action bar *"<boss> xuất hiện sau N giây"*.
2. **Đóng**: mọi người chơi survival/adventure đang đứng trong đấu trường và đạt điều kiện trở thành **người tham gia**. Ít hơn `min_participants` → huỷ. Boss được spawn, cooldown / lịch kế tiếp bắt đầu tính. Action bar chuyển thành *"<boss> \| Thời gian còn lại: 14:32"* (đỏ ở phút cuối).
3. **Trong trận**: chết, rời đấu trường hoặc thoát game = **bị loại**, không quay lại được. Người ngoài không vào được.
4. **Kết thúc**:

| Kết quả | Khi nào | Thưởng |
| :--- | :--- | :--- |
| Chiến thắng | Boss chết | Mỗi người tham gia **còn sống và đang online** lúc encounter phát thưởng được roll `loot_table` riêng. |
| Toàn diệt | Mọi người tham gia bị loại | Không. |
| Hết giờ | Hết `fight_timeout_seconds` | Không. Boss bị xoá. |
| Huỷ | Thiếu người, admin huỷ, server tắt, boss bị unload | Không. Boss bị xoá, chi phí triệu hồi không hoàn. |

Mỗi boss chỉ có **1 trận cùng lúc** trên toàn server: đang tập hợp hoặc đang đánh thì không triệu hồi / xuất hiện thêm được.

## Lệnh

Quyền: `roguesmp.worldboss.admin`. Id boss có ký tự đặc biệt (`/`, `:`) thì đặt trong ngoặc kép.

| Lệnh | Tác dụng |
| :--- | :--- |
| `/worldboss start <id>` | Mở trận ngay, bỏ qua cooldown, không cần chi phí hay điều kiện người triệu hồi. |
| `/worldboss cancel <id>` | Huỷ trận đang diễn ra. |
| `/worldboss gui <id>` | Mở GUI triệu hồi (chỉ boss `summoned`). |
| `/worldboss list` | Trạng thái mọi boss: đang diễn ra, cooldown, lần xuất hiện kế tiếp, và `[disabled]` nếu bị khoá. |
| `/worldboss disable <id\|all>` | Khoá boss (hoặc mọi boss). |
| `/worldboss enable <id\|all>` | Mở khoá boss (hoặc gỡ khoá toàn bộ). |

### Khoá world boss

Admin khoá boss bằng `/worldboss disable`. Danh sách khoá lưu trong `global_config.json`, key `disabledWorldBosses` (giữ nguyên sau khi khởi động lại; `"*"` = mọi boss):

```json
"disabledWorldBosses": ["goblin_king"]
```

Boss bị khoá:

- **Không triệu hồi được** — GUI hiện *"Tạm thời bị khóa"*.
- **Không tự xuất hiện** — lịch chu kỳ vẫn giữ giờ cũ; mở khoá khi đã quá giờ thì boss xuất hiện ở lần kiểm kế tiếp (≤ 20 giây).
- **Trận đang diễn ra không bị ảnh hưởng** — dùng `/worldboss cancel` nếu cần dừng.
- **`/worldboss start` vẫn chạy được** (admin bỏ qua khoá).

`enable <id>` chỉ gỡ khoá của riêng boss đó; nếu đang `disable all` thì boss vẫn bị khoá cho tới khi `enable all`.
