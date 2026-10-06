# JSON Reference — Crafting recipes

Thư mục: `crafting_recipes/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Đây là hệ công thức riêng của plugin (bàn chế tạo tùy chỉnh `CraftingGui` 3×3 và bàn thờ hợp nhất — altar), không phải recipe vanilla.

## Field chung mọi công thức

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `type` | string | bắt buộc | `shaped`, `shapeless`, `fusion`. |
| `id` | string | bắt buộc | Id công thức. Nên bằng đường dẫn file. |
| `result` | [ingredient](#ingredient--nguyên-liệu--kết-quả) | gần như bắt buộc | Kết quả. Codec cho phép thiếu, nhưng thiếu thì công thức không cho ra gì (shaped/shapeless hiện ô chắn; fusion báo "không có công thức"). `count` của nó = số lượng item ra. |

`type: "scrapping"` có class trong code nhưng **chưa đăng ký** → fail cả file; không dùng.

<a id="ingredient--nguyên-liệu--kết-quả"></a>
## Ingredient — nguyên liệu / kết quả

Có 2 cách viết:

```json
"minecraft:iron_ingot"                       // dạng ngắn: số lượng = 1
{ "item": "fire_sword", "count": 2 }          // dạng đầy đủ: count optional, mặc định 1
```

`item`: bắt đầu bằng `minecraft:` → item vanilla (theo Material); không có tiền tố → id item tùy chỉnh trong `items/`. Khi so khớp, item thật của người chơi quy về id tùy chỉnh của nó, nếu không có thì `minecraft:<material>`; so khớp theo chuỗi chính xác. **Không hỗ trợ tag** (`#...` không khớp gì).

## `shaped` — theo khuôn

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `pattern` | mảng string | bắt buộc | Các hàng của khuôn. |
| `key` | object: **ký tự → ingredient** | bắt buộc | Mỗi ký tự trong `pattern` → nguyên liệu. |
| `mirrored` | boolean | optional, `true` | Cũng khớp khi lật gương ngang. |
| `remainders` | object: **key ingredient → key để lại** | optional, `{}` | Vật để lại khi ô bị dùng hết, vd. `"minecraft:water_bucket": "minecraft:bucket"`. |

Quy tắc khuôn:
- Bàn chế tạo là **3×3** → khuôn lớn hơn 3×3 **không bao giờ khớp**. Codec không kiểm tra kích thước.
- Chiều rộng = độ dài hàng đầu; hàng ngắn hơn được đệm khoảng trắng, hàng dài hơn bị cắt.
- **Khoảng trắng = ô trống**, nên không dùng ký tự cách làm ký hiệu. Ký tự không có trong `key` thành ô trống im lặng. Key của `key` nên là 1 ký tự.
- Cả khuôn lẫn lưới người chơi được cắt về khung bao nhỏ nhất → vị trí đặt không quan trọng. Khuôn toàn trống không bao giờ khớp.
- `count` của ingredient trong `key` = số item **tối thiểu** ở ô đó (có dư thì phần dư giữ lại).

```json
{
  "type": "shaped",
  "id": "fire_sword_recipe",
  "result": { "item": "fire_sword", "count": 1 },
  "pattern": [" I ", " I ", " S "],
  "key": {
    "I": { "item": "minecraft:blaze_rod", "count": 1 },
    "S": "minecraft:stick"
  },
  "mirrored": true
}
```

## `shapeless` — không theo khuôn

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `ingredients` | mảng ingredient | bắt buộc | Nguyên liệu, vị trí không quan trọng. |

Mỗi phần tử là **1 yêu cầu riêng**: liệt kê `gunpowder` 2 lần = cần **2 ô** có thuốc súng (không tự cộng dồn số lượng). Lưới phải có **đúng bằng** số ô có đồ cho từng loại; thừa 1 ô cùng loại hoặc có loại không liệt kê → không khớp. Mỗi ô có thể chứa nhiều hơn yêu cầu (phần dư giữ lại).

```json
{
  "type": "shapeless",
  "id": "gunpowder_mix",
  "result": { "item": "minecraft:tnt" },
  "ingredients": ["minecraft:gunpowder", "minecraft:gunpowder", "minecraft:sand"]
}
```

<a id="fusion--bàn-thờ-hợp-nhất-altar"></a>
## `fusion` — bàn thờ hợp nhất (altar)

| Key | Kiểu | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `input` | ingredient | bắt buộc | Item đặt ở bàn thờ **chính** (giữa). |
| `ingredients` | mảng ingredient | bắt buộc | Item đặt ở các bàn thờ phụ xung quanh. |

Khớp **chính xác**, không phải "ít nhất":
- `input`: đúng key **và đúng số lượng** (stack 5 không khớp `count` 1).
- Các bàn thờ phụ: **tổng** số lượng theo từng loại item (cộng dồn qua cả 8 bàn) phải **bằng đúng** tổng trong `ingredients` (các phần tử cùng item được gộp cộng lại); thừa loại/thừa số lượng → không khớp. Vị trí bàn nào không quan trọng.

Khi nghi lễ xong (~80 tick): trừ đúng số lượng cần ở các bàn phụ (phần dư của stack giữ lại), item ở bàn chính **bị thay bằng `result`**.

Cấu trúc vật lý: 1 block `altar_main` + 8 block `altar_side` ở bán kính 3 (xem [JSON-Blocks](JSON-Blocks.md)). Cần đủ 8 bàn phụ đã nạp, nếu không báo "Chưa đủ 8 altar phụ xung quanh."

```json
{
  "type": "fusion",
  "id": "molten_core",
  "input": "steel_ingot",
  "ingredients": [
    { "item": "minecraft:blaze_rod", "count": 4 },
    { "item": "minecraft:magma_cream", "count": 4 }
  ],
  "result": { "item": "molten_core", "count": 1 }
}
```

## Dành cho dev

Loại công thức đa hình dispatch trên `"type"`: class implement `CraftingRecipe` + `CODEC` (gộp `CraftingRecipe.BASE_CODEC`), đăng ký trong [`CraftingRecipes`](../src/main/java/com/roguesmp/crafting/recipe/CraftingRecipes.java). `CraftingManager` (singleton) dựng `Trie` tra nhanh theo mẫu nguyên liệu từ mọi công thức đã load; reload thư mục này tự dựng lại. GUI: `gui/crafting/CraftingGui`, `FusionGui` (kế thừa `ReactiveGui`, xem [GUI Framework](GUI-Framework.md)).

---
◀ [JSON-Loot](JSON-Loot.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Blocks](JSON-Blocks.md)
