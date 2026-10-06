# JSON Reference — Blocks

Thư mục: `blocks/` (đệ quy, id = đường dẫn bỏ `.json`, **không** có field `"id"`). Thư mục liên quan: `fuel_types/`. Quy tắc chung: [JSON-Overview](JSON-Overview.md).

Block tùy chỉnh **không phải block Bukkit thật**: nó là 1 entity `ItemDisplay` (hiển thị bằng model của resource pack) đặt tại 1 tọa độ và được plugin theo dõi. Item đặt ra block qua component [`block_place`](JSON-Items.md#block_place) (`"block": "<id file trong blocks/>"`).

## Cấu trúc file

```json
{
  "type": "generator",
  "hardness": 6,
  "tools": ["pickaxe"],
  "break_strength": 2,
  "model": "smp:steel_generator",
  "display_name": "<gold>Steel Generator",
  "drops": [ { "item": "steel_generator" } ],
  "data": { "place": "minecraft:stone", "module_slots": 3 }
}
```

Các key chung nằm **phẳng ở cấp ngoài cùng** cạnh `type`; dữ liệu riêng của từng loại block nằm trong object lồng `data`.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `type` | string | `"block"` | Loại block (bảng dưới). Id lạ → lỗi kèm danh sách loại hợp lệ. |
| `data` | object | mặc định của loại | Dữ liệu riêng (chỉ `generator` có). Với loại không có dữ liệu thì bị bỏ qua. |
| `hardness` | int | `2` | Độ cứng, xem [công thức đào](#công-thức-đào). |
| `tools` | mảng id `item_types` | `[]` | Dụng cụ được phép đào. |
| `break_strength` | int | `0` | Chỉ số `BREAK_STRENGTH` tối thiểu của người chơi để đào được. |
| `model` | Key | `minecraft:stone` | Model resource pack (`item_model`) hiển thị trên block; cũng là icon trong GUI generator. Key sai ký tự làm fail file. |
| `display_name` | string MiniMessage | `""` | Tên block — chỉ dùng trong GUI generator. |
| `drops` | mảng drop | `[]` | Rơi khi người chơi phá (chỉ ngoài creative). |
| `experience` | object | không rơi | XP rơi khi phá. |
| `place_sound` | Key | `block.stone.place` | Âm thanh đặt (kênh BLOCKS). |
| `break_sound` | Key | `block.stone.break` | Âm thanh phá. |

Mẹo âm thanh: âm thanh cũng phát phía client, nên nếu item/model của block là 1 block đặt được vanilla sẽ nghe trùng 2 lần — dùng model/base không phải block.

### Loại block (`type`)

| `type` | `data` | Mô tả |
| :--- | :--- | :--- |
| `block` (mặc định) | — | Block trơ: chỉ phá/rơi đồ/XP theo JSON. |
| `tally` | — | Block ví dụ: click phải tăng bộ đếm, hiện trên action bar. |
| `generator` | [GeneratorData](#generator--máy-phát) | Máy phát tài nguyên. |
| `altar_main` | — | Bàn thờ chính (giữa) của hợp nhất — xem [JSON-Crafting](JSON-Crafting.md#fusion--bàn-thờ-hợp-nhất-altar). |
| `altar_side` | — | Bàn thờ phụ (1 trong 8 bàn xung quanh). |

Trạng thái runtime của block (bộ đếm tally, vật đặt trên altar, kho/năng lượng generator) lưu trong SQLite `blocks.db` — **không** phải JSON designer viết.

<a id="công-thức-đào"></a>
### Công thức đào

Chỉ áp dụng ở **sinh tồn**. `miningSpeed = 1 + chỉ số MINING_SPEED + 20 × cấp (Haste hoặc Conduit Power, lấy cao hơn) − phạt Mining Fatigue`, tối thiểu 0 (= không đào được).
- `hardness < 0`: plugin không nhận sát thương đào → coi như không phá được bằng tay.
- `hardness = 0`, hoặc `miningSpeed > 30 × hardness`: phá tức thì.
- Còn lại: thời gian đào = `max(4, round(30 × hardness / miningSpeed))` tick.

`break_strength`: tổng `BREAK_STRENGTH` của người chơi phải ≥ giá trị này, nếu không **không đào được**.

`tools`: mỗi phần tử là id 1 file trong `item_types/` (xem [JSON-Items](JSON-Items.md#item_types--loại-item)). Để trống = cầm gì cũng đào được. Có giá trị: item cầm tay chính phải là **item tùy chỉnh** thuộc tag của **ít nhất 1** loại liệt kê; item vanilla không bao giờ khớp. Id sai bị bỏ qua — nếu **mọi** id đều sai, block không đào được.

### `drops` — danh sách rơi

Mỗi phần tử **roll độc lập**:

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `item` | string | bắt buộc | `minecraft:<material>` (vanilla) hoặc id item tùy chỉnh trong `items/`. Id không hợp lệ → không rơi gì, không báo lỗi. |
| `min_amount` | int | `1` | Số lượng tối thiểu. |
| `max_amount` | int | `1` | Số lượng tối đa. Nếu `max ≤ min` lấy `min`. |
| `chance` | double | `1.0` | Xác suất rơi (0–1). |

### `experience`

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `min_amount` | int | `0` | XP tối thiểu. |
| `max_amount` | int | `0` | XP tối đa; `≤ 0` = không rơi XP. |
| `chance` | double | `1.0` | Xác suất có XP. |

<a id="generator--máy-phát"></a>
## `generator` — máy phát

Đặt 1 block (`place`) ở ô trước mặt, tự "đào" nó, thu vào kho nội bộ, lặp lại — tiêu tốn năng lượng. `data`:

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `place` | string | `minecraft:stone` | Block được sinh ra: `minecraft:<material>` hoặc id file trong `blocks/`. Hướng ô sinh = hướng nhìn của người đặt (mặc định EAST). |
| `place_delay` | int tick | `100` | Chờ bao lâu trước khi đặt block mới. |
| `break_delay` | int tick | `100` | Thời gian "đào" mỗi block; tiêu năng lượng mỗi tick. |
| `drops` | mảng drop | `[]` | Rơi **của máy** (không phải của block được sinh), roll mỗi lần thu hoạch. Drop có `chance < 1.0` được tính là "hiếm", `chance = 1.0` là "thường" (module dùng phân biệt này). |
| `max_energy` | int | `5000` | Trần năng lượng. |
| `energy_per_tick` | int | `1` | Năng lượng đốt mỗi tick đào. |
| `capacity` | int | `1000` | Tổng số item kho chứa được; dư thì rơi ra đất. |
| `module_slots` | int | `0` | Số ô module (GUI hiện tối đa 5). `0` = không lắp được module. |

Vòng lặp: chỉ chạy khi năng lượng > 0; chờ `place_delay` → đặt → đào `break_delay` → thu hoạch → chờ lại. Ô đích bị vật khác chiếm thì máy chờ và thử lại. Nhiên liệu được nạp qua GUI.

```json
{
  "type": "generator",
  "hardness": 6,
  "tools": ["pickaxe"],
  "break_strength": 2,
  "model": "smp:steel_generator",
  "display_name": "<gradient:#aaa:#fff>Steel Generator",
  "place_sound": "block.metal.place",
  "break_sound": "block.metal.break",
  "drops": [ { "item": "steel_generator" } ],
  "data": {
    "place": "minecraft:stone",
    "place_delay": 40,
    "break_delay": 60,
    "max_energy": 8000,
    "energy_per_tick": 2,
    "capacity": 2000,
    "module_slots": 3,
    "drops": [
      { "item": "minecraft:cobblestone", "min_amount": 1, "max_amount": 3 },
      { "item": "minecraft:coal", "min_amount": 1, "max_amount": 2, "chance": 0.4 },
      { "item": "raw_steel", "chance": 0.05 }
    ]
  }
}
```

Item đặt máy (`items/steel_generator.json`):
```json
{ "id": "steel_generator", "base": "STONE",
  "components": { "name": "<gold>Steel Generator", "block_place": { "block": "steel_generator" } } }
```

## Chỉ số, phép toán và hiệu ứng của máy

Module và nhiên liệu đều dùng chung 1 "payload hiệu ứng" (**phẳng** trong object component, không bọc trong key `effect`):

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `modifiers` | mảng modifier | `[]` | Thay đổi chỉ số. |
| `behaviors` | mảng behavior | `[]` | Hành vi đặc biệt. |

### Modifier

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `stat` | enum | bắt buộc | Chỉ số bị ảnh hưởng (bảng dưới). |
| `operation` | enum | optional, `MULTIPLY_BASE` | Cách áp dụng. |
| `amount` | double | bắt buộc | Lượng thay đổi. Âm = xấu đi (với delay/năng lượng/tick: âm = nhanh/rẻ hơn). |

`stat`: `PLACE_DELAY`, `BREAK_DELAY`, `ENERGY_PER_TICK`, `MAX_ENERGY`, `LOOT_CAPACITY` (=`capacity`), `DROP_AMOUNT` (số lượng mọi drop), `RARE_DROP_CHANCE` (chỉ drop `chance < 1.0`, kết quả ép 0–1), `COMMON_DROP_AMOUNT` (cộng thêm cho drop `chance = 1.0`, ngoài `DROP_AMOUNT`).

`operation`:

| Giá trị | `amount` nghĩa là | Ví dụ lore |
| :--- | :--- | :--- |
| `MULTIPLY_BASE` | Phân số, **cộng dồn** giữa các nguồn: `0.25` = +25% | `+25%` |
| `MULTIPLY_TOTAL` | Nhân sau cùng, mỗi modifier 1 hệ số `(1 + amount)`: `0.5` = ×1.5 | `×1.5` |
| `ADD` | Số phẳng cộng cuối (tick, năng lượng, item) | `+3` |

Công thức: `giá trị = gốc × max(0.1, (1 + Σ MULTIPLY_BASE) × Π(1 + MULTIPLY_TOTAL)) + Σ ADD`. Chỉ số nguyên được làm tròn (và chỉ số gốc > 0 không xuống dưới 1); `energy_per_tick` ≥ 0, **không làm tròn** (nợ lẻ được cộng dồn nên `0.5`/tick hoạt động); số lượng drop có phần lẻ được roll theo xác suất; hệ số tổng không dưới 0.1.

### Behavior

Field chọn loại: `"type"`. Dùng được ở module, synergy và nhiên liệu (`behaviors`). Hiện có 2 loại:

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `convert_drops` | `conversions` (object: **id item → id item**, bắt buộc) | Sau khi roll drop, thay id item theo bảng (giữ số lượng), trước khi vào kho. Key dạng `item` giống `drops`. |
| `add_drops` | `drops` (mảng drop, bắt buộc) | Mỗi lần thu hoạch roll thêm các drop này (cùng định dạng `drops` của block: `item`, `min_amount`, `max_amount`, `chance`). Drop `chance < 1` tính là hiếm nên chịu `RARE_DROP_CHANCE`; mọi drop chịu `DROP_AMOUNT`. |

```json
{ "type": "convert_drops", "conversions": { "minecraft:cobblestone": "minecraft:iron_ore" } }
{ "type": "add_drops", "drops": [ { "item": "minecraft:diamond", "chance": 0.05 } ] }
```

Behavior chạy lần lượt: các module (kèm synergy của từng module) trước, nhiên liệu sau. `convert_drops` chỉ đổi những drop đã có tại thời điểm nó chạy, nên `convert_drops` của module **không** đổi drop do `add_drops` của nhiên liệu thêm vào; ngược lại `convert_drops` của nhiên liệu đổi được cả drop do module thêm.

<a id="generator_module--item-module"></a>
## `generator_module` — item module

Component đặt trên 1 item (trong `items/`) để biến nó thành module lắp vào máy phát.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `modifiers`, `behaviors` | mảng | `[]` | Hiệu ứng khi module được lắp. |
| `incompatible` | mảng id item | `[]` | Module xung đột (kiểm tra cả 2 chiều); hiện lore "Xung đột: ...". |
| `max_per_machine` | int | `0` | Tối đa bao nhiêu bản trong 1 máy; `0` = không giới hạn. |
| `synergies` | mảng synergy | `[]` | Hiệu ứng thêm khi đang đốt 1 loại nhiên liệu nhất định. |

Synergy: `type` (id file trong `fuel_types/`, bắt buộc) + `modifiers`/`behaviors` phẳng trong cùng object. Chỉ hoạt động khi nhiên liệu **loại đó** đang cháy (nhiên liệu có `duration > 0`, không tính nhiên liệu chỉ cấp năng lượng). Module bị rơi ra đất khi phá máy. Lỗi lắp: không phải module, hết ô, xung đột, đạt giới hạn.

```json
{
  "id": "overclock_module",
  "base": "PAPER",
  "components": {
    "name": "<red>Overclock Module",
    "generator_module": {
      "modifiers": [
        { "stat": "BREAK_DELAY", "operation": "MULTIPLY_BASE", "amount": -0.25 },
        { "stat": "ENERGY_PER_TICK", "operation": "MULTIPLY_BASE", "amount": 0.5 }
      ],
      "max_per_machine": 1,
      "incompatible": ["eco_module"],
      "synergies": [ { "type": "wood", "modifiers": [ { "stat": "DROP_AMOUNT", "amount": 0.5 } ] } ]
    }
  }
}
```

<a id="generator_fuel--item-nhiên-liệu"></a>
## `generator_fuel` — item nhiên liệu

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `energy` | int | `0` | Năng lượng cộng mỗi item. Tự tiêu thụ đủ số item vừa với phần năng lượng còn trống (`floor((max − hiện tại)/energy)`). |
| `modifiers`, `behaviors` | mảng | `[]` | Hiệu ứng tạm khi nhiên liệu đang "cháy". |
| `duration` | int | `0` | Thời gian cháy mỗi item, tính theo `unit`. **`0` = không có hiệu ứng** (`modifiers`/`behaviors` bị bỏ qua). |
| `unit` | enum | `TICKS` | `TICKS` (đếm mỗi tick khi máy còn năng lượng) hoặc `HARVESTS` (đếm mỗi lần thu hoạch). |

Ba kiểu dùng: **chỉ năng lượng** (`energy > 0`, `duration 0`); **chỉ hiệu ứng** (`energy 0`, `duration > 0`: tiêu đúng 1 item và chỉ khi chính nhiên liệu đó chưa cháy); **cả hai** (tiêu item và bắt đầu/kéo dài cháy `duration × số item`).

```json
{
  "id": "coal_briquette",
  "base": "COAL",
  "components": {
    "name": "<gray>Coal Briquette",
    "generator_fuel": { "energy": 400, "duration": 1200, "unit": "TICKS",
      "modifiers": [ { "stat": "PLACE_DELAY", "amount": -0.2 } ],
      "behaviors": [ { "type": "add_drops", "drops": [ { "item": "minecraft:flint", "chance": 0.1 } ] } ] }
  }
}
```

<a id="fuel_types--loại-nhiên-liệu"></a>
## `fuel_types/` — loại nhiên liệu

Nhóm các item nhiên liệu thành "loại" để module synergy nhắm tới và để lore hiển thị.

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `tag` | string | bắt buộc | Tên tag item (`items/tags/<tag>.json`); **chữ thường**. Item nằm trong tag là nhiên liệu loại này. |
| `display` | string MiniMessage | bắt buộc | Tên hiển thị trong lore ("Loại nhiên liệu: ..." / "Với nhiên liệu ...:"). |

```json
// fuel_types/wood.json  (id "wood")
{ "tag": "wood_fuel", "display": "<#a0522d>Gỗ" }
```
```json
// items/tags/wood_fuel.json
["coal_briquette", "oak_log_item"]
```

## Bàn thờ altar

Cấu trúc hợp nhất: 1 `altar_main` + 8 `altar_side` đặt ở bán kính 3 quanh nó, cùng độ cao, mỗi 45° (EAST = +x, SOUTH = +z; chéo ở `(±2, ±2)`). Mỗi altar là 1 file `blocks/*.json` với `type` tương ứng và chỉ có key chung (không `data`):

```json
{ "type": "altar_main", "hardness": 8, "model": "smp:fusion_altar",
  "display_name": "<light_purple>Fusion Altar", "drops": [ { "item": "fusion_altar" } ] }
```
Cách chơi: click phải với item để đặt cả stack lên altar (đồ cũ trả lại); tay không trên altar phụ lấy lại; trên altar chính mở GUI hợp nhất (Shift + tay không để lấy lại item). Phá altar rơi item đang đặt. Công thức: [JSON-Crafting → fusion](JSON-Crafting.md#fusion--bàn-thờ-hợp-nhất-altar).

## Dành cho dev

`BlockData.CODEC` đọc `type` (mặc định `block`), tra `Registries.BLOCK_TYPE` rồi dùng codec dữ liệu của loại đó. Loại block đăng ký trong [`BlockTypes`](../src/main/java/com/roguesmp/block/BlockTypes.java) (`register("<type id>", Class::new, dataCodec)`); class `extends SmpBlock` (hook: `onPlaced`, `getDrops`, `getExperience`, `onBlockBreak`, `onBlockInteract`, `onUnload`, `onHydrated`; state persist khai qua `collectSections`, đánh dấu `markDirty()`). Implement thêm `Tickable` (`void tick()`) để được `BlockManager` tick mỗi tick (opt-in): hiện `ResourceGeneratorBlock` và `AltarBlock` dùng, `TallyBlock`/`SmpBlock` thì không. Behavior generator đăng ký trong `Registries.GENERATOR_BEHAVIOR_CODEC`. Block là subsystem được viết lại nhiều lần gần đây — nếu thấy lệch mô tả, đọc lại `block/` trực tiếp.

---
◀ [JSON-Crafting](JSON-Crafting.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Dungeons](JSON-Dungeons.md)
