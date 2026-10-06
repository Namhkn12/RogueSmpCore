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
        { "type": "item", "weight": 5, "item_id": "ruby_shard", "min_amount": 1, "max_amount": 3,
          "conditions": [ { "type": "chance", "chance": 0.5 } ] },
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
| `conditions` | mảng condition | optional, `[]` | Entry chỉ là ứng viên khi mọi condition đúng (weight coi như 0 nếu sai). |
| `functions` | mảng | optional, `[]` | ⚠️ **Hiện chưa có function nào được đăng ký** — chỉ dùng `[]` hoặc bỏ key; mọi phần tử khác làm fail cả file. (Ghi chú `set_count` ở tài liệu cũ không tồn tại.) |

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
| `chance` | `chance` (double, ngay trên object) | Roll ngẫu nhiên `< chance`. **Roll lại ở mỗi lần đánh giá** (mỗi roll, mỗi entry), nên cùng 1 pool có thể đúng lần này sai lần sau. |
| `origin` | `origins` (mảng enum) | Nguồn mở loot nằm trong danh sách: `CHEST`, `ENTITY`, `BLOCK`, `QUEST`, `COMMAND`, `UNKNOWN` (mặc định khi nơi gọi không khai nguồn). |
| `and` | `conditions` (mảng) | **Mọi** condition con đúng (rỗng = đúng). |
| `or` | `conditions` (mảng) | **Ít nhất 1** đúng (rỗng = sai). |
| `not` | `condition` (**1** object, không phải mảng) | Đảo ngược condition. |

Nhớ phân biệt `conditions` (số nhiều, mảng — `and`/`or`) và `condition` (số ít, 1 object — `not`).

## Thuật toán roll

1. Sự kiện `LootRollEvent` bắn ra; plugin khác hủy được → kết quả rỗng.
2. Duyệt từng **pool** theo thứ tự. Nếu bất kỳ condition cấp pool sai → bỏ pool.
3. Lặp `max(1, rolls)` lần: tính trọng số từng entry (0 nếu condition sai), loại ứng viên có trọng số ≤ 0, chọn ngẫu nhiên theo trọng số. Tổng trọng số = 0 → lần roll đó không ra gì.
4. Mỗi lần roll **đánh giá lại** condition (xem `chance`).

## Ví dụ — loot boss

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [ { "type": "item", "item_id": "bosses/crown", "weight": 1 } ],
      "conditions": [ { "type": "chance", "chance": 0.05 } ]
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

Thêm entry/condition mới: tạo class + `CODEC` (entry gộp `LootEntry.BASE_CODEC`), đăng ký trong `LootEntries` / `LootConditions` (`LootFunctions` hiện trống). Tên thư mục lấy từ hằng số `LootConfig.LOOT_TABLE_FOLDER = "loottable"`. Luồng roll: `LootService` (bắn `LootRollEvent`, `LootPoolPickEvent`, `LootRollCompleteEvent` để plugin khác can thiệp).

---
◀ [JSON-Npcs](JSON-Npcs.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Crafting](JSON-Crafting.md)
