# Loot System

Package: [`com.roguesmp.loot`](../src/main/java/com/roguesmp/loot)

Hệ loot table kiểu Minecraft (pools chứa entries có trọng số, mỗi pool/entry có thể gắn điều kiện) nhưng shape JSON **không giống** loot table vanilla — đọc kỹ ví dụ bên dưới trước khi copy cấu trúc từ Minecraft. Dùng bởi cả [Dungeon System](Dungeon-System.md) (phần thưởng cuối màn, rương kho báu) và [Entity System](Entity-Boss-Spell-System.md#tham-khảo-đầy-đủ--mọi-component-entity-đã-đăng-ký) (`loot_table` component — mob rơi đồ).

## `LootTable` / `LootPool`

Registry `Registries.LOOT_TABLE = new Registry<>(LootConfig.LOOT_TABLE_FOLDER, LootTable.CODEC)` — hằng số `LootConfig.LOOT_TABLE_FOLDER = "loottable"`, nên thư mục thật là `loottable/`, **không phải** `loot_tables/` hay `loottables/`.

```java
LootTable  { pools: [LootPool] }                                    // "pools" bắt buộc
LootPool   { rolls: int (optional, mặc định 1),
             entries: [LootEntry] (bắt buộc),
             conditions: [LootCondition] (optional, mặc định []) }  // gate cả pool
```

## `LootEntry` — 3 kiểu đã đăng ký

Mọi entry đều có sẵn base field: `weight` (int, optional, mặc định 1), `conditions` (mảng `LootCondition`, optional), `functions` (mảng `LootFunction`, optional — **hiện chưa có kiểu `LootFunction` nào được đăng ký**, để đó cho tương lai, đừng viết `"functions"` với 1 `"type"` chưa tồn tại).

| `type` | Field riêng | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item_id` (String), `min_amount`/`max_amount` (int, optional, mặc định 1) | Rơi ra `item_id`, số lượng roll ngẫu nhiên trong khoảng |
| `loot_table` | `name` (String — id 1 `LootTable` khác) | Gộp thêm kết quả của 1 loot table lồng bên trong |
| `empty` | (không field riêng) | Không rơi gì — dùng để "pha loãng" tỉ lệ trúng của các entry khác trong cùng pool |

## `LootCondition` — 5 kiểu đã đăng ký

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `chance` | `chance` (double, bare — field tên `chance` chứ không lồng thêm object) | Có `chance` xác suất áp dụng entry/pool này |
| `origin` | `origins` (mảng String — enum `LootOrigin`: `CHEST, ENTITY, BLOCK, QUEST, COMMAND, UNKNOWN`) | Chỉ áp dụng nếu nguồn mở loot khớp 1 trong các origin liệt kê |
| `and` | `conditions` (mảng `LootCondition`) | Đúng khi **mọi** condition con đều đúng |
| `or` | `conditions` (mảng `LootCondition`) | Đúng khi **ít nhất 1** condition con đúng |
| `not` | `condition` (1 `LootCondition`, không phải mảng) | Đảo ngược 1 condition |

## Ví dụ đầy đủ — `loottable/hell_knight_common.json`

```json
{
  "pools": [
    {
      "rolls": 2,
      "conditions": [ { "type": "origin", "origins": ["CHEST", "QUEST"] } ],
      "entries": [
        {
          "type": "item", "weight": 5,
          "item_id": "ruby_shard", "min_amount": 1, "max_amount": 3,
          "conditions": [ { "type": "chance", "chance": 0.5 } ]
        },
        { "type": "loot_table", "weight": 1, "name": "dungeons/dungeon_a_bonus" },
        { "type": "empty", "weight": 10 }
      ]
    }
  ]
}
```

Đọc bảng trên: pool này roll 2 lần (chỉ khi nguồn mở là rương hoặc quest); mỗi lần roll chọn 1 trong 3 entry theo trọng số (5 / 1 / 10 trên tổng 16), và nếu trúng entry `item` thì còn phải qua thêm 1 lần roll `chance: 0.5` mới thực sự rơi ra `ruby_shard`.

## Cách thêm 1 loại entry/condition/function mới

1. Tạo class implement `LootEntry`/`LootCondition`/`LootFunction`, khai `public static final Codec<YourType> CODEC` — với entry, nhớ gộp base field dùng `LootEntry.BASE_CODEC` (tương tự cách `SmpEffect.BASE_CODEC` được gộp vào từng effect con, xem [Codec System](Codec-System.md#giả-lập-kế-thừa-bằng-1-mapcodec-field-chung)).
2. Đăng ký vào [`LootEntries.java`](../src/main/java/com/roguesmp/loot/entry/LootEntries.java) / [`LootConditions.java`](../src/main/java/com/roguesmp/loot/condition/LootConditions.java) / [`LootFunctions.java`](../src/main/java/com/roguesmp/loot/function/LootFunctions.java) (mỗi file có `loadClass()` để ép static init chạy).
3. Dùng `"type": "your_id"` trong `loottable/*.json`.

## Lưu ý & lỗi thường gặp

- **Thư mục thật là `loottable/`** (số ít, không gạch dưới) — `LootConfig.LOOT_TABLE_FOLDER` là nguồn sự thật duy nhất, đừng đoán tên theo quy ước "số nhiều + gạch dưới" của các registry khác.
- **`"functions"` hiện luôn nên để trống/bỏ hẳn** — `LootFunctions.loadClass()` chưa đăng ký bất kỳ kiểu nào, viết `"functions": [{"type": "..."}]` với bất kỳ type nào cũng sẽ fail decode toàn bộ entry đó.
- **`not` nhận 1 `LootCondition` đơn (field `condition`), không phải mảng** — khác với `and`/`or` (field `conditions`, số nhiều, mảng) — dễ gõ nhầm số ít/số nhiều.
- **`chance` là field bare ngay trên object condition** (`{"type": "chance", "chance": 0.3}`), không lồng thêm 1 object con nào khác.

---
◀ [NPC System](Npc-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Crafting System](Crafting-System.md)
