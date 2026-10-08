# JSON Reference — Loot tables

Thư mục: **`loottable/`** (số ít, không gạch dưới; đệ quy, id = đường dẫn bỏ `.json`, vd. `dungeons/crypt_reward`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Loot table **không có** field `"id"`.

Dùng bởi: mob chết ([`loot_table` của entity](JSON-Entities.md#loot_table)), rương/phần thưởng dungeon (`lootTableId`, rương kho báu), quest/lệnh. Shape JSON khác loot table vanilla — đừng copy từ Minecraft.

## Cấu trúc file

```json
{
  "pools": [
    {
      "rolls": 2,
      "conditions": [ { "type": "origin", "origins": ["CHEST", "ENTITY"] } ],
      "entries": [
        { "type": "item", "weight": 5, "item_id": "ruby_shard", "min_amount": 1, "max_amount": 3 },
        { "type": "item", "weight": 2, "item_id": "minecraft:golden_apple" },
        { "type": "loot_table", "weight": 1, "name": "dungeons/bonus" },
        { "type": "empty", "weight": 10 }
      ]
    }
  ]
}
```

### Table

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `pools` | mảng pool | bắt buộc | Các pool; **mỗi pool được roll độc lập**, theo thứ tự. |

### Pool

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `rolls` | int | optional, `1` | Số lần roll (tối thiểu 1). |
| `entries` | mảng entry | bắt buộc | Các ứng viên. |
| `conditions` | mảng condition | optional, `[]` | Điều kiện của **cả pool**: sai → bỏ qua pool, không roll lần nào. |

### Entry (chung)

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `type` | string | bắt buộc | `item`, `loot_table`, `empty`. |
| `weight` | int | optional, `1` | Trọng số trúng. Trúng với xác suất `weight / tổng weight của các entry đủ điều kiện`. |
| `quality` | double | optional, `0` | Độ nhạy với `luck`: weight hiệu dụng = `weight * max(0, 1 + luck * quality)`. `luck` 0.35 = +35%. Dương cho item hiếm, âm cho rác, `0` = không bị ảnh hưởng. |
| `conditions` | mảng condition | optional, `[]` | Entry chỉ là ứng viên khi mọi condition đúng (weight coi như 0 nếu sai). |
| `functions` | mảng | optional, `[]` | Chạy tuần tự lên ItemStack vừa sinh ra. Xem bảng bên dưới. |

### Loại entry

| `type` | Key riêng | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item_id` (string, bắt buộc), `min_amount` (int, `1`), `max_amount` (int, `1`) | Rơi item. `item_id` dạng `minecraft:<material>` → item vanilla thường (vật liệu lạ = không rơi gì); id trần → item tùy chỉnh trong `items/` (id lạ = không rơi gì). Số lượng ngẫu nhiên đều trong `[min, max]`; nếu `max ≤ min` lấy `min`; luôn ≥ 1. |
| `loot_table` | `name` (string, bắt buộc) | Roll thêm 1 loot table khác (id) và gộp kết quả. Lồng tối đa **8 tầng** (sâu hơn bị dừng im lặng). Table không tồn tại = không rơi gì. |
| `empty` | — | Không rơi gì; chỉ tốn trọng số, dùng để "pha loãng" tỉ lệ. |

### Condition

Field chọn loại: `"type"`.

| `type` | Key | Đúng khi |
| :--- | :--- | :--- |
| `origin` | `origins` (mảng enum) | Nguồn mở loot nằm trong danh sách: `CHEST`, `ENTITY`, `BLOCK`, `QUEST`, `COMMAND`, `UNKNOWN` (mặc định khi nơi gọi không khai nguồn). |
| `and` | `conditions` (mảng) | **Mọi** condition con đúng (rỗng = đúng). |
| `or` | `conditions` (mảng) | **Ít nhất 1** đúng (rỗng = sai). |
| `not` | `condition` (**1** object, không phải mảng) | Đảo ngược condition. |

Nhớ phân biệt `conditions` (số nhiều, mảng — `and`/`or`) và `condition` (số ít, 1 object — `not`).

## Functions

| `type` | Key | Tác dụng |
| :--- | :--- | :--- |
| `set_count` | `min`, `max` (int, mặc định 1) | Ghi đè amount bằng số ngẫu nhiên trong `[min, max]`. |
| `looting` | `max` (int, =1), `limit` (int, không giới hạn) | Cộng thêm `random(0..max) * looting` vào amount (làm tròn), tối đa `limit`. `looting` lấy từ context của nơi roll; bằng 0 thì không làm gì. Chỉ entry nào gắn function này mới scale. |

## Luck & looting

Hai giá trị do nơi gọi roll đặt vào context: `luck` (đổi xác suất qua `quality`) và `looting` (đổi số lượng qua function `looting`). Debug bằng `/template loottable roll <table> [luck] [looting]` và `/template loottable odds <table> [luck] [looting]`.

## Thuật toán roll

1. Duyệt từng **pool** theo thứ tự. Nếu bất kỳ condition cấp pool sai → bỏ pool.
2. Lặp `max(1, rolls)` lần: tính trọng số từng entry (`weight * max(0, 1 + luck * quality)`, loại nếu condition sai), loại ứng viên có trọng số ≤ 0, chọn ngẫu nhiên theo trọng số. Tổng trọng số = 0 → lần roll đó không ra gì.

## Ví dụ — loot boss

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "item", "item_id": "bosses/crown", "weight": 1 },
        { "type": "empty", "weight": 19 }
      ]
    },
    {
      "rolls": 3,
      "entries": [
        { "type": "item", "item_id": "ruby_shard", "min_amount": 2, "max_amount": 5, "weight": 6 },
        { "type": "item", "item_id": "minecraft:diamond", "weight": 2 },
        { "type": "empty", "weight": 4 }
      ]
    }
  ]
}
```

## Dành cho dev

Thêm entry/condition mới: tạo class + `CODEC` (entry gộp `LootEntry.BASE_CODEC`), đăng ký trong `LootEntries` / `LootConditions` (`LootFunctions` hiện trống). Tên thư mục lấy từ hằng số `LootConfig.LOOT_TABLE_FOLDER = "loottable"`. Luồng roll nằm trong `LootService`; không có event nào can thiệp, kết quả trả về là list bất biến.

---
◀ [JSON-Npcs](JSON-Npcs.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Crafting](JSON-Crafting.md)
