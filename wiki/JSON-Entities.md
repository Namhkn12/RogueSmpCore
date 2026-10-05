# JSON Reference — Entities (mob/boss)

Thư mục: `entities/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Tag entity (`elite`, `boss`, `angelic`, `friendly`) ở `entities/tags/`.

## Cấu trúc 1 file entity

```json
{
  "id": "goblin_chief",
  "entityType": "ZOMBIE",
  "components": {
    "display_name": "<red>Goblin Chief",
    "attributes": { "max_health": 200 }
  }
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Id entity. **Phải bằng đường dẫn file** (`entities/bosses/x.json` → `"bosses/x"`): id này được ghi vào mob lúc spawn và dùng để nạp lại mob sau khi chunk reload; lệch → mob mất hành vi sau reload. Dungeon tham chiếu entity theo id này. |
| `entityType` | enum `EntityType` của Bukkit | bắt buộc | Loại mob. **Phải là sinh vật sống** (ZOMBIE, SKELETON, VILLAGER, WITHER, MANNEQUIN, ...). Loại không sống (`ITEM`, `ARROW`, `MARKER`, ...) decode được nhưng **lỗi lúc spawn**; `PLAYER`/`UNKNOWN` không spawn được. |
| `components` | object | optional, `{}` | **Key = tên component** (bảng dưới). Tên lạ (kể cả `phase`) → fail cả file. |

Mọi mob tự có 1 `nameplate` mặc định nếu file không khai.

## Bảng component

| Key | Dạng | Ý nghĩa ngắn |
| :--- | :--- | :--- |
| [`display_name`](#display_name) | string | Tên trên bảng tên |
| [`behavior`](#behavior) | object | AI, bất tử, despawn, tầm phát hiện |
| [`attributes`](#attributes) | object key tự do | Chỉ số (máu, tốc độ, sát thương...) |
| [`equipment`](#equipment) | object key tự do | Trang bị |
| [`spells`](#spells) | object | Spell chủ động / bị động |
| [`boss_bar`](#boss_bar) | object | Thanh máu boss |
| [`nameplate`](#nameplate) | object | Bảng tên + máu trên đầu |
| [`loot_table`](#loot_table) | mảng string | Loot table khi chết |

### `display_name`
String MiniMessage trần. Dòng tên trên bảng tên (nameplate). Không khai → hiện tên loại mob (`ZOMBIE`). Không đặt `customName` vanilla.
```json
"display_name": "<red>Hell Knight"
```

### `behavior`
Tất cả optional.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `noAi` | boolean | `false` | `true` = tắt AI (đứng yên, không đánh). |
| `invulnerable` | boolean | `false` | Bất tử. |
| `persistent` | boolean | `false` | `true` = được lưu, không bao giờ despawn. `false` = despawn bình thường khi xa. |
| `detectionRange` | int (block) | `20` | Bán kính "đang hoạt động" của **spell**: khi không có người chơi (không phải spectator) trong bán kính này, spell bị động không chạy và vòng xoay spell chủ động dừng/hủy. Giá trị ≤ 0 bị đổi thành 20 — không thể đặt "luôn hoạt động". Không ảnh hưởng AI vanilla. Không khai `behavior` → cũng là 20. |

```json
"behavior": { "persistent": true, "detectionRange": 25 }
```

### `attributes`
Object key tự do: **tên attribute → số**. Đặt giá trị gốc (base) của attribute vanilla tương ứng; `max_health` còn đặt luôn máu hiện tại. Attribute mob không có sẵn được tự đăng ký.

Key hợp lệ (không phân biệt hoa/thường): `max_health`, `follow_range`, `knockback_resistance`, `movement_speed`, `flying_speed`, `attack_damage`, `attack_knockback`, `attack_speed`, `armor`, `fall_damage_multiplier`, `safe_fall_distance`, `scale`, `step_height`, `gravity`, `jump_strength`, `burning_time`, `explosion_knockback_resistance`, `movement_efficiency`, `water_movement_efficiency`.

**Lenient**: entry sai tên/sai giá trị bị bỏ + log warning, phần còn lại vẫn áp dụng.
```json
"attributes": { "max_health": 200, "movement_speed": 0.3, "attack_damage": 15 }
```

### `equipment`
Object key tự do: **tên ô → đồ**. Ô là enum `EquipmentSlot` của Bukkit: `HAND` (tay chính — **không** phải `MAIN_HAND`), `OFF_HAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`, `BODY`, `SADDLE`. Xác suất rơi đồ luôn bị ép về 0 (loot chỉ đến từ `loot_table`). **Lenient**: ô sai/đồ sai (vd. material lạ) bị bỏ + log, ô đó để trống.

Mỗi đồ:

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `material` | Material | bắt buộc | Vật liệu. |
| `displayName` | string MiniMessage | optional | Tên item. |
| `lore` | mảng string MiniMessage | optional | Lore. |
| `enchantGlint` | boolean | optional, `false` | Luôn áp dụng: `true` bật lấp lánh, `false` ép tắt. |
| `trimMaterial` | string | optional | Vật liệu trim vanilla, không namespace (`gold`, `redstone`, `netherite`, ...). |
| `trimPattern` | string | optional | Mẫu trim vanilla, không namespace (`sentry`, `silence`, ...). **Chỉ áp dụng khi có cả `trimMaterial` lẫn `trimPattern`.** |
| `dyeColor` | màu `"A,R,G,B"` | optional | Nhuộm màu (giáp da...). Sai định dạng → **lỗi lúc spawn** (không phải lúc load). |
| `headSkin` | string | optional | Id trong `skins/` (cho đầu người). Id lạ bị bỏ qua. |

Luôn áp dụng: item bất hủy (unbreakable), xóa attribute modifier vanilla.
```json
"equipment": {
  "HAND": { "material": "NETHERITE_SWORD", "displayName": "<red>Hellfire Blade", "enchantGlint": true },
  "HEAD": { "material": "NETHERITE_HELMET", "trimMaterial": "redstone", "trimPattern": "silence" }
}
```

### `spells`
Tất cả optional.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `activeSpell` | mảng [spell](#danh-sách-spell) | `[]` | Vòng xoay spell cast lần lượt từng cái (tự chọn ngẫu nhiên spell sẵn sàng). |
| `passiveSpell` | mảng [spell](#danh-sách-spell) | `[]` | Spell chạy mỗi chu kỳ thụ động, hoặc phản ứng theo sự kiện. |
| `passiveInterval` | int (tick) | `0` | Chu kỳ chạy spell thụ động. ≤ 0 → 2 tick. |
| `canCastSameSpellTwice` | boolean | `false` | ⚠️ Tên key gây nhầm: giá trị này được truyền thẳng vào cờ "chặn cast cùng 1 spell liên tiếp". Thực tế `true` = **cấm** cast lặp, `false` = cho phép (có vẻ là lỗi đặt tên trong code). Hãy thử trong game nếu cần chắc. |

Cách vòng chủ động chạy: bắt đầu sau 1 tick, kiểm tra mỗi 2 tick; sau khi cast, đợi `cooldownTicks` của spell đó; mỗi lần chọn 1 spell sẵn sàng ngẫu nhiên (spell vừa cast phải nghỉ khoảng `floor((số spell − 1)/2)` lượt chọn); không có spell nào chạy được → đợi 20 tick. **Liệt kê cùng 1 loại spell 2 lần trong `activeSpell` chỉ còn 1.** Mỗi lần cast bắn sự kiện `SpellCastEvent`. Spell phản ứng sự kiện (`self_destruct_spell`, `dummy_entity_spell`, aspect...) hoạt động bất kể `detectionRange`.

1 `type` spell lạ hoặc 1 phần tử sai làm **fail cả file entity**.

```json
"spells": {
  "activeSpell": [ { "type": "self_heal_spell" }, { "type": "shadow_step_spell" } ],
  "passiveSpell": [ { "type": "fire_aspect_spell" }, { "type": "self_destruct_spell", "particleCount": 20 } ],
  "passiveInterval": 4
}
```

### `boss_bar`
Tất cả optional.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `range` | int (block) | `30` | Người chơi trong bán kính này (cùng world) thấy thanh máu. Cập nhật mỗi 2 tick. |
| `color` | enum | `WHITE` | `PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE`. |
| `style` | enum | `PROGRESS` | `PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20`. (Key là `style`, không phải `overlay`.) |
| `bossFog` | boolean | `true` | Thêm sương mù thế giới + làm tối màn hình. |

Tiêu đề thanh = tên mob (chữ thường, không MiniMessage); tiến độ = máu/máu tối đa; ẩn khi chết.
```json
"boss_bar": { "range": 40, "color": "RED", "style": "NOTCHED_10" }
```

### `nameplate`
Bảng tên nổi (1 `TextDisplay` cưỡi trên mob). Tất cả optional.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `showHealth` | boolean | `true` | Thêm dòng `hiện tại/tối đa❤` (xanh ≥ 75%, vàng ≥ 40%, đỏ dưới đó). |
| `showName` | boolean | `true` | Thêm dòng tên (`display_name`). |
| `heightOffset` | double (block) | `0.3` | Độ cao bảng tên so với mob. |

Cả 2 `show*` = `false` → không có bảng tên. Cập nhật 1 tick sau mỗi lần nhận sát thương.
```json
"nameplate": { "showHealth": false, "heightOffset": 0.5 }
```

### `loot_table`
Mảng string trần = id các loot table trong `loottable/` (xem [JSON-Loot](JSON-Loot.md)). Khi mob chết, **mỗi** table được roll với origin `ENTITY` (và người giết là người chơi nếu có), đồ rơi tại chỗ chết.
```json
"loot_table": ["mobs/goblin_chief", "mobs/common"]
```
- Id table không tồn tại: chỉ log mức debug → **thất bại im lặng**.
- Mọi mob do plugin quản lý đều bị **xóa toàn bộ drop vanilla** khi chết (kể cả mob không có `loot_table`); chỉ kinh nghiệm vanilla được giữ.

<a id="danh-sách-spell"></a>
## Danh sách spell

Phần tử của `activeSpell`/`passiveSpell`. Field chọn loại: **`"type"`**. Spell không tham số viết gọn `{ "type": "..." }`. "Dùng trong" là gợi ý, không được kiểm tra.

| `type` | Key riêng | Dùng trong | Tác dụng |
| :--- | :--- | :--- | :--- |
| `self_destruct_spell` | `particleCount` (int, `10`) | passive | Khi mob chết: đổi âm thanh chết thành tiếng nổ + tạo `particleCount` particle nổ. Không gây sát thương. |
| `slow_aura_spell` | — | passive | Mỗi chu kỳ, người chơi trong 5 block bị làm chậm 30% (100 tick, làm mới liên tục). Bán kính/mức cố định. |
| `fire_aspect_spell` | — | passive | Mỗi đòn mob gây ra đốt cháy nạn nhân 3 giây. |
| `ice_aspect_spell` | — | passive | Mỗi đòn mob gây ra đóng băng nạn nhân 2 giây. |
| `blind_spell` | — | passive | Mỗi đòn mob gây ra làm mù nạn nhân (cấp 5, 3 giây). ⚠️ Giả định nạn nhân là người chơi — đánh vào mob khác sẽ lỗi `ClassCastException`; chỉ dùng cho mob đánh người chơi. |
| `fire_resistance_spell` | — | passive | Mỗi chu kỳ cho mob kháng lửa II (500 tick, làm mới liên tục). |
| `dummy_entity_spell` | — | cả hai | Hình nhân tập đánh: mỗi lần bị đánh, hồi đầy máu 1 tick sau; người đánh thấy "Damage dealt: X"; Shift + tay không để xóa hình nhân. |
| `self_heal_spell` | — | active | Hồi 20% lượng máu đã mất. Hồi chiêu 60 tick. |
| `shadow_step_spell` | — | active | Nếu đang nhìn 1 người chơi trong 20 block: dịch chuyển ra sau lưng họ 1.5 block. Hồi chiêu 50 tick. Nhìn mục tiêu không phải người chơi → không làm gì. |
| `death_grip_spell` | — | active | Nếu đang nhìn 1 người chơi trong 20 block: kéo họ tới 1 điểm ngẫu nhiên cách mob 3 block. Hồi chiêu 60 tick. |

## Ví dụ hoàn chỉnh — `entities/bosses/goblin_chief.json`

```json
{
  "id": "bosses/goblin_chief",
  "entityType": "ZOMBIE",
  "components": {
    "display_name": "<red>Goblin Chief",
    "behavior": { "persistent": true, "detectionRange": 25 },
    "attributes": { "max_health": 200, "movement_speed": 0.28, "attack_damage": 12 },
    "equipment": { "HAND": { "material": "IRON_SWORD", "enchantGlint": true } },
    "boss_bar": { "range": 40, "color": "RED", "style": "NOTCHED_10" },
    "spells": {
      "activeSpell": [ { "type": "self_heal_spell" }, { "type": "shadow_step_spell" } ],
      "passiveSpell": [ { "type": "fire_aspect_spell" } ],
      "passiveInterval": 4
    },
    "loot_table": ["mobs/goblin_chief"]
  }
}
```
Thêm id vào `entities/tags/boss.json` (`["bosses/goblin_chief"]`) để được tính là boss.

## Tag entity — tác dụng thực tế

| Tag | Ai đọc | Hiệu ứng |
| :--- | :--- | :--- |
| `boss`, `elite` | enchant `REGICIDE`; ability `shield_bash` | Regicide chỉ có tác dụng với mục tiêu elite/boss; ShieldBash xử lý riêng mục tiêu boss/elite. |
| `friendly` | ability `split_arrow` | Bỏ qua projectile do entity `friendly` bắn. |
| `angelic` | (chưa có nơi nào đọc) | Dự phòng, hiện không có tác dụng. |

## Dành cho dev

Component entity là hệ mirror của item component: [`EntityComponentKeys`](../src/main/java/com/roguesmp/entity/component/EntityComponentKeys.java) đăng ký codec (`register("id", X.CODEC)`), component implement `EntityComponent` (+ `TickingComponent` nếu cần tick). `phase` là component code-only (`BossHealthAction` là lambda, không JSON-hóa được). Thêm spell: tạo `SpellParams` (record + `CODEC`) + class `Spell`, đăng ký trong `EntitySpells`. Boss viết tay đăng ký factory ở `SpecialEntities` (hiện toàn bộ đang comment — mọi mob chạy như `SmpEntity` thường). Kiến trúc: [Entity, Boss & Spell System](Entity-Boss-Spell-System.md).

---
◀ [JSON-Items](JSON-Items.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Abilities](JSON-Abilities.md)
