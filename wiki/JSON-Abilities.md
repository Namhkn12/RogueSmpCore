# JSON Reference — Abilities

Thư mục: `ability_info/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

Mỗi ability gồm **2 nửa**: phần **code** (Java: hành vi, các "action" có tên) và phần **JSON** (file ở đây: mô tả, chỉ số theo level, phím kích hoạt, chi phí nâng cấp). Chúng nối với nhau **chỉ qua id**: `ability_info/fireball.json` ↔ ability có `ID = "fireball"` trong code. File **không có** field `"id"`/`"type"`. Không có file → ability chạy với cấu hình rỗng (icon `BARRIER`, không phím kích hoạt, không chỉ số = mọi giá trị đọc ra `0`). Lệch id không báo lỗi gì.

## Cấu trúc file

```json
{
  "display_name": "<red>Fireball",
  "icon": "FIRE_CHARGE",
  "description": ["Phóng cầu lửa gây <damage> sát thương.", "Hồi chiêu: <cooldown:second>s"],
  "scaling": {
    "damage": [10, 14, 18],
    "cooldown": [100, 90, 80]
  },
  "trigger": {
    "execute": { "key": "RIGHT_CLICK", "options": ["sneaking"] }
  },
  "upgrades": {
    "2": [ { "type": "exp", "level": 10 } ],
    "3": [ { "type": "item", "item_id": "fire_essence", "amount": 5 } ]
  }
}
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `display_name` | string MiniMessage | `""` | Tên hiển thị; rỗng → dùng id ability. |
| `icon` | Material | `BARRIER` | Icon trong GUI. |
| `description` | mảng string MiniMessage | `[]` | Mô tả; hỗ trợ tag giá trị (bảng dưới). |
| `scaling` | object: **tên chỉ số → mảng số** | `{}` | Giá trị theo level. Phần tử đầu = level 1. |
| `trigger` | object: **tên action → trigger** | `{}` | Phím kích hoạt. Rỗng = ability bị động (GUI hiện "Kích hoạt: Bị động"). |
| `upgrades` | object: **`"<level đích>"` → mảng yêu cầu** | `{}` | Chi phí lên từng level. |

### `scaling` — chỉ số theo level

- Level bắt đầu từ **1**; giá trị level N = phần tử thứ N, nếu level vượt độ dài mảng thì dùng phần tử cuối.
- Tên chỉ số (`damage`, `cooldown`...) **do code của ability đọc** — phải đúng tên mà code dùng (xem [bảng ability](#ability-có-sẵn)). Tên không có trong code thì vô tác dụng; thiếu tên mà code đọc → giá trị `0`.
- **Level tối đa = độ dài mảng dài nhất** trong `scaling`.
- Mảng nên có độ dài bằng nhau; thời gian tính bằng tick (xem `<key:second>` để hiển thị giây).

### `description` — tag giá trị

| Tag | Hiển thị |
| :--- | :--- |
| `<damage>` | Giá trị chỉ số `damage` của level hiện tại (tên bất kỳ có trong `scaling`) |
| `<cooldown:second>` hoặc `:s` | Chia 20 (tick → giây) |
| `<damage_boost:percent>` hoặc `:p` | Nhân 100 (0.25 → 25) |
| `<level>` | Level hiện tại |

Khi xem trước nâng cấp, giá trị đổi hiển thị dạng ~~cũ~~ » mới.

### `trigger` — phím kích hoạt

Key của map là **tên action** do code đăng ký (vd. `execute`, `cast`, `activate`) — không đặt tùy ý. Mỗi giá trị:

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `key` | enum | bắt buộc | `LEFT_CLICK`, `RIGHT_CLICK`, `SWAP` (phím đổi tay), `SNEAK`, `JUMP`. |
| `options` | mảng string | optional, `[]` | Điều kiện phụ — **tất cả** phải đúng. |
| `displayName` | string | optional, `""` | Nhãn trong dòng "Kích hoạt (...)" khi 1 ability có nhiều trigger. (camelCase) |

`options` hợp lệ — chỉ có 4 giá trị (phân biệt hoa/thường): `sneaking` (đang ngồi), `not_sneaking`, `sprinting` (đang chạy), `not_sprinting`. Id lạ **bị bỏ qua im lặng** (coi như điều kiện đúng).

Lưu ý: ability có nhiều action mà bạn không khai trigger cho action đó thì action đó không có phím. Nếu 2 trigger cùng khớp 1 phím, thứ tự chọn là ngẫu nhiên (map không có thứ tự). Ability "hook-driven" (phản ứng sự kiện, không có action) bỏ qua `trigger` — chỉ dùng để hiển thị.

### `upgrades` — chi phí nâng cấp

Key là **chuỗi số level đích** (`"2"`, `"3"`), giá trị là mảng yêu cầu **phải thỏa hết**. `"2"` = chi phí từ level 1 lên 2.

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item_id` (id item tùy chỉnh trong `items/`, bắt buộc), `amount` (int, bắt buộc) | Nộp `amount` item; quét toàn bộ túi đồ và trừ đi. |
| `exp` | `level` (int, bắt buộc) | Tiêu tốn lượng kinh nghiệm tương đương `level` cấp vanilla (trừ thật, không chỉ kiểm tra). |

Quan trọng:
- **Phải có entry `upgrades["<level kế tiếp>"]`** thì mới nâng được. Thiếu entry → GUI báo "đã tối đa" dù `scaling` còn dài. Entry là mảng rỗng `[]` = nâng miễn phí.
- Chỉ nhận **item tùy chỉnh** (không dùng `minecraft:`).
- Yêu cầu sai bị bỏ + log (lenient) → có thể làm nâng cấp rẻ hơn dự định; kiểm tra log.
- `item_id` sai → cảnh báo tham chiếu treo sau khi load.

<a id="ability-có-sẵn"></a>
## Ability có sẵn

Id → tên chỉ số trong `scaling` mà code đọc → tên action (khóa của `trigger`). "hook" = không có action, phản ứng theo sự kiện.

| Id | Chỉ số `scaling` | Action |
| :--- | :--- | :--- |
| `dagger_throw` | `dagger_count`, `damage`, `range`, `cooldown`, `damage_boost`, `damage_boost_duration`, `silence_duration`, `recast_duration`, `recast_multiplier` | `cast` |
| `bodkin_blitz` | `cooldown`, `stealth_duration`, `bonus_damage`, `bonus_damage_duration`, `distance`, `charges` | `cast` |
| `advancing_shadow` | `range`, `cooldown`, `duration`, `dmgBonus` | `cast` |
| `cloak_of_shadows` | `cooldown`, `radius`, `slowness_amplifier`, `stealth_duration`, `slowness_duration`, `damage` | `cast` |
| `armor_breaker` | `duration`, `max_stack`, `bonus_per_stack` | hook |
| `dodging` | `cooldown_ticks` | hook |
| `shield_bash` | `damage`, `cooldown`, `stun_duration`, `range`, `radius`, `knockback_strength` | hook |
| `glorious_battle` | `cooldown`, `velocity`, `collision_radius`, `collision_damage`, `collision_knockback`, `landing_damage`, `landing_knockback`, `landing_radius`, `resistance_amplifier`, `resistance_duration`, `stun_duration` | `cast` |
| `indomitable` | `damage_resistance`, `health_increase`, `knockback_resistance` | hook |
| `brute_force` | `radius`, `damage` | hook |
| `split_arrow` | `range`, `bounce`, `multiplier` | hook |
| `sharpshooter` | `passive_dmg`, `stack_dmg`, `cooldown_decay` | `execute` (không làm gì) |
| `scrapshot` | `damage`, `range`, `cooldown` | `execute` |
| `sidearm` | `damage`, `cooldown`, `max_stacks` | `execute` |
| `gravity_bomb` | `radius`, `damage`, `cooldown` | `execute` |
| `fireball` | `damage`, `radius`, `velocity`, `cooldown` | `execute` |
| `flamestrike` | `damage`, `radius`, `cooldown` | `execute` |
| `igneous_rune` | `damage`, `radius`, `cooldown` | `execute` |
| `volcanic_meteor` | `impact_damage`, `radius`, `cooldown` | `execute` |
| `pyroblast` | `damage`, `radius`, `cooldown` | hook |

Có class trong code nhưng **chưa đăng ký** nên file `ability_info` cho chúng không có tác dụng và không được liệt kê vào `default_abilities`: `aether_stance`, `firework_blast`, `flame_spirit`, `infernal_overdrive`, `raygun`, `last_breath`.

## Ví dụ hoàn chỉnh — `ability_info/glorious_battle.json` (mẫu cấu trúc)

```json
{
  "display_name": "<gold>Glorious Battle",
  "icon": "GOLDEN_AXE",
  "description": [
    "Lao tới, gây <collision_damage> sát thương khi va chạm.",
    "Hồi chiêu: <cooldown:second>s"
  ],
  "scaling": {
    "cooldown": [400, 360, 320],
    "velocity": [1.2, 1.3, 1.4],
    "collision_radius": [2, 2, 2.5],
    "collision_damage": [8, 10, 12],
    "collision_knockback": [1, 1, 1.2],
    "landing_damage": [6, 8, 10],
    "landing_knockback": [1, 1, 1.2],
    "landing_radius": [3, 3, 4],
    "resistance_amplifier": [1, 1, 2],
    "resistance_duration": [60, 80, 100],
    "stun_duration": [20, 30, 40]
  },
  "trigger": { "cast": { "key": "SWAP" } },
  "upgrades": {
    "2": [ { "type": "exp", "level": 15 } ],
    "3": [ { "type": "item", "item_id": "warrior_emblem", "amount": 3 }, { "type": "exp", "level": 30 } ]
  }
}
```
(Các số chỉ là ví dụ — cân bằng thật do designer quyết định; tên chỉ số lấy đúng từ bảng trên.)

## Dành cho dev

Thêm ability: class extend `Ability` + `AbilityInfo` static (`registerAction("<action>", X::method)`), đăng ký trong [`AbilityInfos`](../src/main/java/com/roguesmp/player/ability/AbilityInfos.java), viết `ability_info/<id>.json`, thêm id vào `default_abilities` của class nhân vật. Chi tiết, luồng cast, `AbilityResponse`, capture: [Player Ability System](Player-Ability-System.md). Thêm option trigger: `TriggerOptions.java`. Thêm loại upgrade requirement: `UpgradeRequirements`.

---
◀ [JSON-Entities](JSON-Entities.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Classes](JSON-Classes.md)
