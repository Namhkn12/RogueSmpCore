# JSON Reference — Warps

Thư mục: `warps/` (id = đường dẫn bỏ `.json`, **không có** field `"id"`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

Warp là điểm dịch chuyển có tên, hiện trong menu `/warp`. Thường không cần viết tay: `/warp create` và GUI editor tự ghi file. Viết JSON khi cần chỉnh giá trị mà GUI không có (thông số hiệu ứng, điểm đến phức tạp).

## Cấu trúc file

`warps/arena.json`:

```json
{
  "display_name": "<red>Đấu trường",
  "icon": "minecraft:netherite_sword",
  "lore": [
    "<gray>Nơi diễn ra trận boss",
    "<yellow>Cấp đề xuất: 30+"
  ],
  "destination": { "type": "region", "region": "boss_arena", "world": "minecraft:spawn_world" },
  "options": { "effect": "enderman", "warmup": 30, "safe": true },
  "permission": "roguesmp.warp.arena",
  "order": 10
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `display_name` | string MiniMessage | **bắt buộc** | Tên hiện trong menu. |
| `icon` | key item model | `minecraft:ender_pearl` | Model của icon (icon là giấy với `item_model` này, nên dùng được cả model custom của resource pack). |
| `lore` | mảng string MiniMessage | `[]` | Mỗi phần tử 1 dòng lore trong menu. |
| `destination` | [điểm đến](#điểm-đến-destination) | **bắt buộc** | Nơi warp dẫn tới. |
| `options` | [tuỳ chọn dịch chuyển](#tuỳ-chọn-dịch-chuyển-options) | xem bảng dưới | Hiệu ứng, thời gian chờ, dịch chuyển an toàn. |
| `permission` | string | không có (mọi người) | Node quyền cần có. Người không có quyền **không thấy** warp trong menu lẫn gợi ý lệnh, và `/warp <id>` báo "không tìm thấy". |
| `order` | int | `0` | Thứ tự trong menu, nhỏ đứng trước. Bằng nhau thì xếp theo id. |

Id warp: chỉ `a-z`, `0-9`, `_`, `-`, dài 1–32 ký tự, và không được là `create`, `set`, `edit`, `delete` (trùng tên subcommand). Quy tắc này chỉ kiểm khi tạo bằng lệnh; file viết tay sai quy tắc vẫn nạp nhưng có thể không gõ được bằng `/warp <id>`.

⚠️ Warp **không được** trỏ tới warp khác hoặc người chơi (`warp`, `player`) — lệnh và GUI từ chối. File viết tay không bị kiểm, nhưng đừng làm vậy.

## Điểm đến (`destination`)

Field chọn loại: `"type"`. Dùng chung cho warp, action `teleport` của [NPC](JSON-Npcs.md#action), và lệnh `/stp`.

| `type` | Key | Đến |
| :--- | :--- | :--- |
| `location` | `x`, `y`, `z` (double, bắt buộc); `world` (key), `yaw`, `pitch` (float) optional | Đúng toạ độ. Bỏ `yaw`/`pitch` = giữ hướng nhìn hiện tại. |
| `region` | `region` (string, bắt buộc); `world` optional | [`smp-spawn`](Region-Flags.md#smp-spawn) của region (không có thì tâm region). |
| `world_spawn` | `world` optional | Điểm spawn của world. |
| `hub` | — | Spawn của hub. |
| `warp` | `warp` (id warp) | Điểm đến của warp đó. |
| `player` | `name` (string) | Vị trí hiện tại của người chơi đó (phải online). |

`world` là **key đầy đủ có namespace** (`minecraft:overworld`, `minecraft:spawn_world`), không phải tên thư mục. **Bỏ `world`** = world người chơi đang đứng lúc dịch chuyển (dùng cho dungeon, world instance có tên ngẫu nhiên).

Điểm đến được tra **lúc dịch chuyển**: đổi `smp-spawn` của region đích thì mọi warp trỏ tới nó tự cập nhật. Không tra được (world chưa nạp, region không có, người chơi offline) → người chơi đứng yên, nhận tin báo đỏ (nếu có hiệu ứng), log ghi cảnh báo. Trong menu, admin thấy dòng "⚠ Destination can't be resolved".

```json
{ "type": "location", "world": "minecraft:overworld", "x": 0.5, "y": 80, "z": 0.5, "yaw": 90, "pitch": 0 }
{ "type": "region", "region": "boss_arena" }
{ "type": "world_spawn", "world": "minecraft:spawn_world" }
{ "type": "hub" }
```

### Dạng chữ (lệnh)

Lệnh `/warp set`, `/stp`, flag `smp-enter-teleport` và ô nhập trong GUI dùng dạng chữ tương đương:

```
region <id> [world] | spawn [world] | hub | warp <name> | player <name> | [world] <x> <y> <z> [yaw pitch]
```

Gõ 1 từ không phải số (vd. `Steve`) = `player Steve`. Riêng `/warp set` có thêm `here` = vị trí hiện tại của bạn.

## Tuỳ chọn dịch chuyển (`options`)

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `effect` | string preset hoặc object | `"enderman"` | Hiệu ứng khi dịch chuyển. |
| `warmup` | int (tick) | `30` (1.5 giây) | Thời gian chờ trước khi dịch chuyển. `0` = ngay lập tức. |
| `safe` | bool | `true` | Dịch chuyển an toàn qua Multiverse (tránh rơi vào block/khoảng không). Multiverse thất bại → dịch chuyển thường. `false` = đúng toạ độ. |

Trong lúc chờ, người chơi không bắt đầu được lần dịch chuyển khác (lần thứ 2 bị bỏ qua).

### `effect`

Preset (viết dạng string): `"enderman"`, `"none"`.

Muốn chỉnh thông số thì viết dạng object với `"type"`:

| `type` | Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `none` | — | — | Không hiệu ứng, không tin báo khi thất bại. |
| `enderman` | `depart_pitch` (float) | `0.6` | Cao độ tiếng `entity.enderman.teleport` lúc bắt đầu chờ. |
| | `arrive_pitch` (float) | `1.4` | Cao độ tiếng lúc tới nơi. |
| | `slowness` (int) | `2` | Cấp Slowness (amplifier, `2` = Slowness III) trong lúc chờ. `-1` = tắt. |
| | `darkness` (int) | `1` | Cấp Darkness trong lúc chờ. `-1` = tắt. |

```json
"options": {
  "effect": { "type": "enderman", "slowness": -1, "darkness": -1, "arrive_pitch": 2.0 },
  "warmup": 0
}
```

Effect bị gỡ nếu dịch chuyển thất bại.

## Lệnh và GUI

| Lệnh | Quyền | Việc |
| :--- | :--- | :--- |
| `/warp` | — | Mở menu warp (chỉ warp bạn có quyền dùng). |
| `/warp <id>` | — | Dịch chuyển tới warp. |
| `/warp create <id>` | `roguesmp.warp.admin` | Tạo warp tại vị trí của bạn (tên mặc định `<aqua><id>`). |
| `/warp set <id> <điểm đến>` | `roguesmp.warp.admin` | Đổi điểm đến ([dạng chữ](#dạng-chữ-lệnh) hoặc `here`). |
| `/warp edit <id>` | `roguesmp.warp.admin` | Mở GUI editor. |
| `/warp delete <id>` | `roguesmp.warp.admin` | Xoá warp và file. |
| `/stp <điểm đến> [enderman\|none]` | `roguesmp.teleport` | Dịch chuyển bản thân. Mặc định ngay lập tức; thêm `enderman` ở cuối để có hiệu ứng. |
| `/stp send <targets> <điểm đến> [enderman\|none]` | `roguesmp.teleport` | Dịch chuyển người chơi khác. |

Trong menu `/warp`, admin có thêm nút tạo warp và **Shift + click** 1 warp để sửa. GUI editor chỉnh được: điểm đến (click trái = vị trí hiện tại, click phải = gõ), icon (click trái = model của item đang cầm), tên, lore, permission, order, effect (chỉ chuyển giữa preset), warmup. Thông số chi tiết của effect chỉ chỉnh được trong JSON.

Mọi thay đổi qua lệnh/GUI được ghi file ngay.

## Dành cho dev

| File | Vai trò |
| :--- | :--- |
| [`warp/Warp`](../src/main/java/com/roguesmp/warp/Warp.java) | Định nghĩa + `CODEC`, registry `Registries.WARP` (`warps/`) |
| [`warp/WarpService`](../src/main/java/com/roguesmp/warp/WarpService.java) | Tạo/sửa/xoá + kiểm tra, ghi file tuần tự theo từng id |
| [`teleport/TeleportService`](../src/main/java/com/roguesmp/teleport/TeleportService.java) | Điểm vào duy nhất để dịch chuyển người chơi: warmup, hiệu ứng, safe teleport |
| [`teleport/TeleportDestinations`](../src/main/java/com/roguesmp/teleport/TeleportDestinations.java) | Đăng ký loại điểm đến (`destination/`) |
| [`teleport/TeleportDestinationParser`](../src/main/java/com/roguesmp/teleport/TeleportDestinationParser.java) | Dạng chữ → điểm đến |
| [`teleport/effect/TeleportEffects`](../src/main/java/com/roguesmp/teleport/effect/TeleportEffects.java) | Đăng ký hiệu ứng + preset |
| [`teleport/SmpTeleportEvent`](../src/main/java/com/roguesmp/teleport/SmpTeleportEvent.java) | Event huỷ được, bắn 2 lần: `WARMUP` (trước khi chờ) và `DEPART` (đã tra điểm đến, sắp dịch chuyển) |

Khi cần dịch chuyển người chơi trong code, gọi `TeleportService.teleport(...)` thay vì `player.teleport(...)` để có hiệu ứng và event thống nhất. Thêm loại điểm đến: implement `TeleportDestination`, đăng ký trong `TeleportDestinations`, thêm nhánh vào parser nếu cần dạng chữ.

---
◀ [JSON-Trades](JSON-Trades.md) · Về [Trang chủ](Home.md)
