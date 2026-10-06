# JSON Reference — Items

Thư mục: `items/` (đệ quy, id = đường dẫn bỏ `.json`). Xem [JSON-Overview](JSON-Overview.md) cho quy tắc chung (id, kiểu giá trị, MiniMessage, lỗi). Trang này cũng bao gồm các thư mục đi kèm: `item_types/`, `skins/`, và ghi chú về enchant.

## Cấu trúc 1 file item

```json
{
  "id": "fire_sword",
  "base": "NETHERITE_SWORD",
  "components": {
    "name": "<gold>Fire Sword",
    "durability": 500
  }
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Id item dùng trong code. Phải bằng đường dẫn file (xem [JSON-Overview](JSON-Overview.md#field-id-trong-file-phải-khớp-id-đường-dẫn)). |
| `base` | Material | bắt buộc | Vật liệu vanilla của stack — quyết định hình dáng mặc định, ô trang bị mặc định, tính chất vanilla. Tooltip/attribute/độ bền vanilla bị plugin xóa và thay bằng dữ liệu từ component. |
| `components` | object | optional, `{}` | **Key = tên component**, value = nội dung của component đó (xem bảng dưới). Chỉ khai component cần dùng. |

Tên component **không có trong bảng** làm fail cả file item. Item có component giữ trạng thái riêng từng cây (`durability`, `enchant`, `socket`, `usage_timer`, `random_stat`, ...) tự động trở thành item "unique" (mỗi item có UUID riêng) — không cần khai gì thêm.

## Bảng tất cả component

| Key | Dạng | Ý nghĩa ngắn |
| :--- | :--- | :--- |
| [`name`](#name) | string | Tên hiển thị |
| [`description`](#description) | mảng string | Dòng lore mô tả |
| [`stack_size`](#stack_size) | int | Số lượng tối đa mỗi stack |
| [`durability`](#durability) | int | Độ bền tối đa (độ bền tùy chỉnh) |
| [`durability_repair`](#durability_repair) | int | Lượng độ bền item này hồi khi dùng để sửa |
| [`magic_power`](#magic_power) | int | Ma lực tối đa |
| [`random_stat`](#random_stat) | `{}` | Mỗi item roll 1 "chất lượng" ngẫu nhiên |
| [`enchant`](#enchant) | object | Enchant tùy chỉnh gốc của item |
| [`attribute`](#attribute) | object | Chỉ số cộng cho người trang bị |
| [`socket`](#socket) | int | Số ô khảm ngọc |
| [`gem_data`](#gem_data) | object | Biến item này thành "ngọc" khảm được |
| [`consumable`](#consumable) | object | Ăn/uống được, kèm hiệu ứng |
| [`potion_content`](#potion_content) | object | Màu + hiệu ứng potion vanilla |
| [`head_skin`](#head_skin) | string | Skin cho `PLAYER_HEAD` |
| [`item_model`](#item_model) | Key | Model resource pack |
| [`equippable`](#equippable) | object | Mặc được vào ô giáp |
| [`glint`](#glint) | boolean | Ép bật/tắt hiệu ứng lấp lánh |
| [`wrench`](#wrench) | `{}` | Cờ lê phá block tùy chỉnh |
| [`block_place`](#block_place) | object | Đặt ra 1 block tùy chỉnh |
| [`passive_ability`](#passive_ability) | mảng object | Năng lực thụ động của item |
| [`usage_timer`](#usage_timer) | int | Item tự biến mất sau khoảng thời gian trang bị |
| [`command_executor`](#command_executor) | string | Chạy lệnh khi click |
| `generator_module` | object | Module cho máy phát — xem [JSON-Blocks](JSON-Blocks.md#generator_module--item-module) |
| `generator_fuel` | object | Nhiên liệu cho máy phát — xem [JSON-Blocks](JSON-Blocks.md#generator_fuel--item-nhiên-liệu) |

### `name`
Chuỗi MiniMessage trần. Đặt tên custom (không in nghiêng mặc định).
```json
"name": "<gold>Fire Sword"
```

### `description`
Mảng chuỗi MiniMessage, mỗi phần tử 1 dòng lore (không in nghiêng).
```json
"description": ["<gray>Thanh kiếm rèn từ lửa rồng.", "<gray>Cẩn thận khi cầm."]
```

### `stack_size`
Số nguyên trần = số item tối đa trong 1 stack.
```json
"stack_size": 16
```

### `durability`
Số nguyên trần = độ bền tối đa. Độ bền hiện tại lưu riêng từng item (mặc định đầy). Plugin giả lập thanh độ bền vanilla và hiện dòng lore "Độ bền: hiện tại/tối đa" (xanh ≥ 75%, vàng ≥ 40%, cam ≥ 15%, đỏ dưới đó). Là điều kiện để dùng `unyielding_edge`.
```json
"durability": 500
```

### `durability_repair`
Số nguyên trần = lượng độ bền hồi lại khi item này được dùng để sửa item khác. Lore hiện "Có thể sử dụng để sửa chữa vật phẩm / Hồi phục N độ bền". (Component chỉ giữ con số; logic sửa nằm ở nơi khác.)
```json
"durability_repair": 100
```

### `magic_power`
Số nguyên trần = ma lực tối đa ("Ma lực"). Giá trị hiện tại do modifier (chất lượng `random_stat`) đặt; dòng lore ẩn khi item có `random_stat`.
```json
"magic_power": 250
```

### `random_stat`
Object rỗng `{}`. Lần đầu item được nạp, roll 1 "chất lượng" ngẫu nhiên 0–1 và lưu vào item; lore hiện "Chất lượng: <hạng> (xx%)" với hạng Hoàn Mỹ (≥100), Mới Cứng (≥80), Tương Đối (≥60), Khả Dụng (≥40), Hoen gỉ (≥20), Tàn tạ.
```json
"random_stat": {}
```

### `enchant`
| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `enchants` | object key tự do: **tên enchant → cấp** (int) | bắt buộc | Enchant có sẵn khi item được tạo. Cấp ≤ 0 bị bỏ qua. Tổng cấp = cấp gốc + cấp người chơi thêm + cộng từ ngọc. |

Key là **tên hằng số enum** `Enchants` (không phân biệt hoa/thường; danh sách ở [mục Enchant](#enchant--không-có-json-riêng)).
```json
"enchant": { "enchants": { "FIRE_ASPECT": 2, "LOOTING": 3 } }
```

### `attribute`
| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `attributes` | object key tự do: **tên attribute → số** (double) | bắt buộc | Chỉ số cộng cho người chơi khi item nằm trong `slot`. |
| `slot` | enum `EquipSlot` | bắt buộc | Ô phải đặt item thì chỉ số mới có hiệu lực. |

`slot` ∈ `MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET, PROJECTILE`. Danh sách attribute ở [mục Attribute](#danh-sách-attribute). Ngọc và enchant có thể cộng thêm lên trên. Lore hiện tiêu đề ô + từng dòng chỉ số khác 0.
```json
"attribute": {
  "slot": "MAINHAND",
  "attributes": { "MELEE_DAMAGE_BASE": 7.0, "ATTACK_SPEED_BASE": -2.4 }
}
```

### `socket`
Số nguyên trần = số ô khảm ngọc. Ngọc đã khảm lưu riêng từng item. Lore: "Còn N ô ngọc trống" + danh sách ngọc đã khảm. Chỉ ngọc có `gem_data` mới có hiệu lực, tối đa bằng số socket.
```json
"socket": 2
```

### `gem_data`
Đặt trên item **ngọc** (không phải trên vũ khí). Item nào có component này khảm được vào item có `socket`.

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `attributes` | object 2 tầng: **`EquipSlot` → (attribute → số)** | bắt buộc | Chỉ số ngọc cho item đích, **tùy ô trang bị của item đích** (khảm vào vũ khí cầm tay → lấy nhánh `MAINHAND`). |
| `success_chance` | double | optional, `1.0` | Tỉ lệ khảm thành công (0–1; giá trị ≤ 0 bị ép về 0). Hiện trong lore (xanh ≥ 0.9, vàng ≥ 0.7, cam ≥ 0.5, đỏ dưới đó). |

```json
"gem_data": {
  "attributes": {
    "MAINHAND": { "CRIT_DAMAGE_FLAT": 3.0 },
    "CHEST": { "MAX_HEALTH_FLAT": 4.0 }
  },
  "success_chance": 0.8
}
```

### `consumable`
Item ăn/uống được. **Cả 8 field đều bắt buộc**, và key viết **camelCase** (khác phần lớn trang còn lại).

| Key | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `effects` | mảng [Effect](#effect--hiệu-ứng-trong-consumable) | Hiệu ứng áp lên người dùng khi ăn xong. Có thể là `[]`. |
| `hunger` | int | Độ no hồi (nutrition). |
| `saturation` | float | Độ bão hòa hồi. |
| `canAlwaysEat` | boolean | `true` = ăn được cả khi đầy bụng. |
| `consumeSeconds` | float | Thời gian ăn/uống (giây). |
| `animation` | enum `ItemUseAnimation` của Paper | Kiểu hoạt ảnh: `EAT`, `DRINK`, `BLOCK`, `BOW`, ... |
| `sound` | Key | Âm thanh khi ăn, vd. `"entity.generic.drink"`. |
| `hasParticles` | boolean | Có particle khi ăn không. |

```json
"consumable": {
  "effects": [ { "id": "speed", "duration": 200, "value": 0.2, "modifierId": "phoenix_elixir" } ],
  "hunger": 0,
  "saturation": 0,
  "canAlwaysEat": true,
  "consumeSeconds": 1.2,
  "animation": "DRINK",
  "sound": "entity.generic.drink",
  "hasParticles": true
}
```

### `potion_content`
Màu + hiệu ứng **potion vanilla** (khác hẳn `consumable.effects`).

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `color` | màu ARGB `"A,R,G,B"` | bắt buộc | Màu chất lỏng. Chuỗi sai định dạng làm fail lúc decode. |
| `effects` | mảng `{type, duration, amplifier}` | bắt buộc | Hiệu ứng vanilla. |

Mỗi hiệu ứng: `type` (string, id hiệu ứng vanilla **không namespace**: `speed`, `regeneration`, ...), `duration` (int tick), `amplifier` (int, bắt đầu từ 0 như vanilla). Cả 3 đều bắt buộc.
```json
"potion_content": {
  "color": "255,0,120,255",
  "effects": [ { "type": "speed", "duration": 1200, "amplifier": 1 } ]
}
```

### `head_skin`
String trần = id 1 skin trong `skins/`. Đặt skin cho item `base: PLAYER_HEAD`. Id không tồn tại → không có tác dụng.
```json
"head_skin": "dragon_helm_skin"
```

### `item_model`
Key trần, trỏ model trong resource pack.
```json
"item_model": "roguesmp:fire_sword"
```

### `equippable`
Cho phép item mặc vào ô giáp (kể cả khi `base` không phải giáp). Bao bọc component vanilla `equippable`.

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `slot` | enum `EquipmentSlot` của Bukkit | bắt buộc | `HAND, OFF_HAND, FEET, LEGS, CHEST, HEAD, BODY, SADDLE`. (Khác enum `EquipSlot` của `attribute`!) |
| `equip_sound` | Key | optional | Âm thanh khi mặc (mặc định vanilla `item.armor.equip_generic`). |
| `asset_id` | Key | optional | Model giáp trong resource pack. |
| `allowed_entities` | mảng Key | optional, `[]` | Loại entity được mặc; rỗng = tất cả. |
| `dispensable` | boolean | optional, `true` | Máy phát (dispenser) mặc được. |
| `swappable` | boolean | optional, `true` | Click phải để hoán đổi giáp. |
| `damage_on_hurt` | boolean | optional, `true` | Mất độ bền khi nhận sát thương. |
| `equip_on_interact` | boolean | optional, `false` | Mặc khi click phải vào entity. |
| `camera_overlay` | Key | optional | Lớp phủ màn hình khi mặc. |
| `can_be_sheared` | boolean | optional, `false` | Cắt bằng kéo lấy giáp ra. |
| `shearing_sound` | Key | optional | Âm thanh khi cắt. |

```json
"equippable": { "slot": "HEAD", "asset_id": "roguesmp:dragon_helm", "damage_on_hurt": false }
```

### `glint`
Boolean trần. `true` ép item luôn lấp lánh, `false` ép tắt.
```json
"glint": true
```

### `wrench`
Object rỗng `{}`. Item là cờ lê: **Shift + click phải** vào block tùy chỉnh để phá/gỡ nó.
```json
"wrench": {}
```

### `block_place`
| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `block` | string | bắt buộc | Id file trong `blocks/` (không phải id item). Click phải đặt block đó, tiêu thụ 1 item (trừ creative; không đặt được ở adventure). |

```json
"block_place": { "block": "steel_generator" }
```

### `passive_ability`
Mảng **item ability** — năng lực thụ động chạy khi item đang được trang bị. Mỗi phần tử có `"type"`; chi tiết ở [mục Item ability](#item-ability).
```json
"passive_ability": [ { "type": "unyielding_edge", "dmgPerUnitLoss": 8.0, "amountLossPerUnit": 150 } ]
```

### `usage_timer`
Số nguyên trần = **tick** item tồn tại (20 tick = 1 giây). Chỉ đếm khi đang **trang bị** (không hoạt động với ô `PROJECTILE`); hết giờ thì thông báo và xóa item. Lore: "Biến mất sau <thời gian> sử dụng...".
```json
"usage_timer": 12000
```

### `command_executor`
String trần = lệnh mặc định (không có `/` đầu). Click phải chạy lệnh bằng quyền người dùng; click trái mở hộp thoại để đổi lệnh (lưu riêng từng item). Chặn dùng/đặt bình thường của item.
```json
"command_executor": "spawn"
```

<a id="danh-sách-attribute"></a>
## Danh sách attribute

Dùng làm key trong `attribute.attributes` và `gem_data.attributes`. Viết đúng tên hằng số (không phân biệt hoa/thường).

| Nhóm | Attribute |
| :--- | :--- |
| Chiến đấu & tấn công | `MELEE_DAMAGE_BASE`, `ATTACK_SPEED_BASE`, `PROJECTILE_DAMAGE_BASE`, `PROJECTILE_SPEED_BASE`, `BREAK_STRENGTH`, `THROW_RATE_BASE`, `MELEE_DAMAGE_PERCENT`, `MAGIC_DAMAGE_PERCENT`, `THROW_RATE_PERCENT`, `PROJECTILE_DAMAGE_PERCENT`, `PROJECTILE_SPEED_PERCENT`, `CRIT_DAMAGE_FLAT`, `ATTACK_KNOCKBACK`, `SWEEPING_DAMAGE_RATIO` |
| Phòng thủ & sinh lực | `DEFENSE_FLAT`, `MAX_HEALTH_FLAT`, `MAX_HEALTH_PERCENT`, `KNOCKBACK_RESISTANCE` |
| Di chuyển trên bộ | `SPEED_FLAT`, `SPEED_PERCENT`, `SNEAKING_SPEED`, `MOVEMENT_EFFICIENCY` |
| Nhảy & vật lý | `JUMP_STRENGTH`, `GRAVITY`, `STEP_HEIGHT`, `FALL_DAMAGE`, `SAFE_FALL_DISTANCE` |
| Dưới nước | `WATER_MOVEMENT_EFFICIENCY`, `SUBMERGED_MINING_SPEED`, `OXYGEN_BONUS` |
| Tiện ích & thế giới | `ENTITY_REACH`, `BLOCK_REACH`, `MINING_EFFICIENCY`, `BURNING_TIME` |
| Đào (block tùy chỉnh) | `MINING_SPEED` (xem công thức ở [JSON-Blocks](JSON-Blocks.md)), `BREAK_STRENGTH` |
| Khác | `SCALE`, `LUCK` |

Hậu tố: `_BASE` = giá trị gốc; `_PERCENT`/`_FLAT` = cộng thêm theo phần trăm/số phẳng. Đơn vị cụ thể của từng attribute hiển thị trong lore trong game.

<a id="enchant--không-có-json-riêng"></a>
## Enchant — không có JSON riêng

Enchant là **enum cứng trong code** (không có thư mục `enchants/` chứa entry). Chỉ có 2 chỗ liên quan JSON:
- Component [`enchant`](#enchant) của item (key = tên hằng số enum, không phân biệt hoa/thường).
- `enchants/tags/<tên>.json` — tag nhóm enchant (mảng id snake_case lowercase hoặc `#tag`).

Danh sách (tên dùng trong component `enchant`): giáp — `FIRE_PROTECTION, BLAST_PROTECTION, PROJECTILE_PROTECTION, REGENERATION, RESPIRATION, AQUA_AFFINITY, THORNS, FEATHER_FALLING, DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED, SWIFT_SNEAK`; vũ khí — `SMITE, BANE_OF_ARTHROPODS, FIRE_SLAYER, IMPALING, REGICIDE, SMASH, BLEEDING, POISONING, CHANNELING, MULTISHOT, QUICK_CHARGE, PIERCING, DENSITY, WIND_BURST, EPOCH, RETRIEVAL, LIFESTEAL, KNOCKBACK, SWEEPING_EDGE, PUNCH, FLAME, FIRE_ASPECT, ICE_ASPECT`; di chuyển — `RIPTIDE, LUNGE`; dụng cụ — `LOOTING, EFFICIENCY, SILK_TOUCH, FORTUNE, LUCK_OF_THE_SEA, LURE, MENDING`; infusion — `VIGOR, FOCUS, FORTITUDE, PERSPICACITY, CELERITY`; thẩm mỹ — `GLOWING`; lời nguyền — `EXHAUSTION, IRREPARABLE, UNCRITABLE`; chỉ để test — `GREED, EXPLOSIVE`.

<a id="effect--hiệu-ứng-trong-consumable"></a>
## Effect — hiệu ứng trong `consumable`

`consumable.effects` là mảng object. **Field chọn loại là `"id"`** (không phải `"type"`). Mọi effect đều có các key nền tảng, cộng key riêng của loại đó trong **cùng 1 object phẳng**:

| Key nền | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Loại effect (bảng dưới). |
| `duration` | int (tick) | bắt buộc | Thời lượng; `-1` = vô hạn. |
| `death_behavior` | enum | optional, `REMOVE_ON_DEATH` | Khi chết: `REMOVE_ON_DEATH` (mất), `KEEP_ON_DEATH` (giữ), `HALVES_ON_DEATH` (còn nửa thời gian). |
| `display_mode` | enum | optional, `WITH_TIME` | Hiện trong danh sách hiệu ứng: `HIDDEN` (ẩn), `WITH_TIME` (kèm thời gian, `--:--` nếu vô hạn), `WITHOUT_TIME`. |

Các loại hợp lệ (mọi `value` là **phân số**, `0.25` = 25%):

| `id` | Key riêng | Tác dụng |
| :--- | :--- | :--- |
| `speed` | `value` (double), `modifierId` (string) — cả 2 bắt buộc | Tăng/giảm tốc chạy theo tỉ lệ (`0.2` = +20%). `modifierId` là khóa modifier — nguồn khác nhau dùng id khác nhau. Giữ qua đăng xuất. |
| `damage_increase` | `increase_value` (double, bắt buộc) | Sát thương gây ra tăng theo tỉ lệ (`0.3` = +30%). |
| `resistance` | `value` (double, bắt buộc), `allowed_damage_types` (mảng enum, optional) | Giảm sát thương nhận (`0.4` = giảm 40%). Mặc định áp cho `MELEE, MELEE_ABILITY, PROJECTILE, PROJECTILE_ABILITY, MAGIC, BLAST`; mảng rỗng = không kháng gì. Giữ qua đăng xuất. |
| `bleeding` | `damage_per_second` (double, bắt buộc) | Mỗi giây gây sát thương `AILMENT` (bỏ qua giáp), kèm particle máu. |
| `potent_poison` | `damage_per_stack` (double, bắt buộc), `stacks` (int, optional, `1`, ép về 1–5) | Mỗi giây gây `damage_per_stack × stacks`. |
| `vulnerability` | `value` (double, bắt buộc) | Nhận thêm sát thương (`0.2` = chịu +20%). |
| `silence` | — | Câm lặng (hệ spell của mob kiểm tra effect này). |
| `empowered_strike` | `value` (double, bắt buộc) | Đòn cận chiến **kế tiếp** mạnh thêm `value`, rồi effect mất. |
| `stealth_effect` | — | Tàng hình: mob/piglin gần đó bỏ mục tiêu. (id là `stealth_effect`, không phải `stealth`.) Giữ qua đăng xuất. |

`DamageType` (cho `allowed_damage_types`): `MELEE, MELEE_ABILITY, PROJECTILE, PROJECTILE_ABILITY, UNSCALEABLE, UNSCALABLE_ABILITY, UNSCALABLE_ENCHANT, MAGIC, THORNS, BLAST, FIRE, FALL, AILMENT, TRUE, OTHER` (chú ý chính tả `UNSCALEABLE` vs `UNSCALABLE_*`).

Các class `stun`, `armor_breaker`, `kb_resist_increase` tồn tại trong code nhưng **chưa đăng ký codec** → không dùng được trong JSON.

```json
{ "id": "resistance", "duration": 1200, "value": 0.25,
  "allowed_damage_types": ["MELEE", "PROJECTILE"],
  "display_mode": "WITHOUT_TIME", "death_behavior": "KEEP_ON_DEATH" }
```

## Item ability

Phần tử của `passive_ability`; field chọn loại là `"type"`. Có 3 loại:

| `type` | Key | Tác dụng |
| :--- | :--- | :--- |
| `unyielding_edge` | `dmgPerUnitLoss` (double, `5.0`), `amountLossPerUnit` (int, `200`) | Mỗi `amountLossPerUnit` độ bền **đã mất**, người cầm được cộng `dmgPerUnitLoss` vào `MELEE_DAMAGE_BASE`. Cần component `durability`; không có thì vô tác dụng. Key viết camelCase. |
| `barking` | `interval` (int tick, `200`, tối thiểu 1), `sounds` (mảng, `[]`) | Mỗi `interval` tick phát 1 âm thanh ngẫu nhiên trong `sounds`, chỉ người cầm nghe. `sounds` rỗng = không làm gì. |
| `entity_zapper` | — | Click phải vào entity → xóa entity khỏi thế giới; click vào người chơi thì chỉ đẩy lùi + báo "NO!". |

Phần tử của `sounds`: `key` (Key, bắt buộc), `volume` (float, `1.0`), `pitch` (float, `1.0`).
```json
"passive_ability": [
  { "type": "unyielding_edge", "dmgPerUnitLoss": 2.5, "amountLossPerUnit": 100 },
  { "type": "barking", "interval": 400, "sounds": [ { "key": "entity.wolf.growl", "volume": 0.6, "pitch": 0.8 } ] },
  { "type": "entity_zapper" }
]
```

<a id="item_types--loại-item"></a>
## `item_types/` — loại item

Mỗi file định nghĩa 1 "loại" gắn với 1 tag item; dùng để (1) in dòng lore "Phân loại: ..." trên item thuộc tag, (2) làm điều kiện dụng cụ đào ở `blocks/*.json` (`tools`).

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `tag` | string | bắt buộc | Id 1 tag item (`items/tags/<tag>.json`). **Viết chữ thường.** |
| `display` | string MiniMessage | bắt buộc | Chữ hiện trong dòng "Phân loại:". |

```json
// item_types/sword.json  (id "sword")
{ "tag": "swords", "display": "<red>Kiếm" }
```
```json
// items/tags/swords.json
["iron_sword", "fire_sword"]
```
Item thuộc nhiều tag khớp nhiều loại sẽ hiện hết, cách nhau dấu phẩy. Item vanilla (không phải item tùy chỉnh) không bao giờ khớp.

<a id="skins--dữ-liệu-skin"></a>
## `skins/` — dữ liệu skin

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `value` | string | bắt buộc | Thuộc tính `textures` (Base64) của Mojang. |
| `signature` | string | bắt buộc | Chữ ký của thuộc tính đó. |
| `uuid` | UUID | optional, sinh ngẫu nhiên lúc load | UUID profile. |

Không cần viết tay: lệnh `/skinfetch <mineskin_uuid> <id>` gọi MineSkin (cần `mineskinApiKey` trong `global_config.json`) và ghi `skins/<id>.json`. Dùng bởi `head_skin`, NPC (`skinId`) và `equipment.headSkin` của entity.

## Ví dụ hoàn chỉnh

Vũ khí có ngọc, enchant, năng lực thụ động:
```json
{
  "id": "weapons/fire_sword",
  "base": "NETHERITE_SWORD",
  "components": {
    "name": "<gold>Fire Sword",
    "description": ["<gray>Thanh kiếm rèn từ lửa rồng."],
    "durability": 500,
    "attribute": { "slot": "MAINHAND", "attributes": { "MELEE_DAMAGE_BASE": 7.0, "ATTACK_SPEED_BASE": -2.4 } },
    "enchant": { "enchants": { "FIRE_ASPECT": 2 } },
    "socket": 2,
    "glint": true,
    "passive_ability": [ { "type": "unyielding_edge", "dmgPerUnitLoss": 5.0, "amountLossPerUnit": 200 } ]
  }
}
```

Ngọc khảm:
```json
{
  "id": "ruby_gem",
  "base": "AMETHYST_SHARD",
  "components": {
    "name": "<red>Ruby",
    "gem_data": { "attributes": { "MAINHAND": { "CRIT_DAMAGE_FLAT": 3.0 } }, "success_chance": 0.8 }
  }
}
```

Mũ trang bị:
```json
{
  "id": "dragon_helm",
  "base": "PLAYER_HEAD",
  "components": {
    "name": "<red>Dragon Helm",
    "head_skin": "dragon_helm_skin",
    "equippable": { "slot": "HEAD", "damage_on_hurt": false },
    "attribute": { "slot": "HEAD", "attributes": { "MAX_HEALTH_FLAT": 20.0, "DEFENSE_FLAT": 8.0 } }
  }
}
```

Bình thuốc ăn được:
```json
{
  "id": "phoenix_elixir",
  "base": "POTION",
  "components": {
    "name": "<light_purple>Phoenix Elixir",
    "stack_size": 16,
    "consumable": {
      "effects": [ { "id": "speed", "duration": 200, "value": 0.2, "modifierId": "phoenix_elixir" } ],
      "hunger": 0, "saturation": 0, "canAlwaysEat": true,
      "consumeSeconds": 1.2, "animation": "DRINK",
      "sound": "entity.generic.drink", "hasParticles": true
    },
    "potion_content": { "color": "255,0,120,255", "effects": [ { "type": "speed", "duration": 1200, "amplifier": 1 } ] }
  }
}
```

## Dành cho dev

Thêm component mới: tạo class implement `ItemComponent` với `public static final Codec<X> CODEC`, đăng ký trong [`ItemComponentKeys`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java) (`X_KEY = register("your_id", X.CODEC)`), rồi viết `"your_id": ...` trong `components`. Kiến trúc đầy đủ: [Item System](Item-System.md). Thêm effect: [Codec System](Codec-System.md#polymorphic-dispatch-codecdispatch), đăng ký trong `EffectCodecs`. Thêm item ability: đăng ký trong `ItemAbilities`.

---
◀ [JSON-Overview](JSON-Overview.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Entities](JSON-Entities.md)
