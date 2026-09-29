# Crafting System

Package: [`com.roguesmp.crafting`](../src/main/java/com/roguesmp/crafting)

1 `CraftingRecipe` là 1 công thức chế tạo tùy chỉnh (không dùng registry recipe vanilla của Bukkit), đa hình trên `"type"` qua [`Registries.CRAFTING_RECIPE_CODEC`](Registry-System.md), load từ thư mục `crafting_recipes/` vào `Registries.CRAFTING_RECIPE`. `CraftingManager` (singleton hand-rolled) index mọi recipe đã load vào 1 `Trie` để tra nhanh theo pattern nguyên liệu — dùng bởi GUI chế tạo riêng (`gui/crafting/CraftingGui`/`FusionGui`, xem [GUI Framework → ReactiveGui](GUI-Framework.md)), không đi qua `MachineGui`/mixin (đã bị xóa).

## Field chung — `CraftingIngredient`

Mọi công thức tham chiếu nguyên liệu/kết quả qua `CraftingIngredient`, chấp nhận 2 dạng viết:

```json
"minecraft:blaze_rod"                          // dạng rút gọn — ngụ ý count = 1
{ "item": "minecraft:blaze_rod", "count": 2 }  // dạng đầy đủ
```

`key` (field `item`) là id `BaseItem` (vd. `"fire_sword"`) hoặc `"minecraft:<material>"` cho item vanilla.

Mọi `CraftingRecipe` đều có 2 field nền tảng: `id` (String, bắt buộc) và `result` (`CraftingIngredient`, optional — `null` cho kiểu không có 1 output duy nhất).

## 3 kiểu đã đăng ký (dùng được ngay)

### `shaped` — theo khuôn 3x3

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
  "mirrored": true,
  "remainders": { "minecraft:water_bucket": "minecraft:bucket" }
}
```

`pattern` (mảng String, tối đa 3 hàng × 3 ký tự, bắt buộc), `key` (map ký tự → `CraftingIngredient`, bắt buộc), `mirrored` (boolean, optional, mặc định `true` — công thức soi gương vẫn khớp), `remainders` (map ingredient-key → ingredient-key còn lại sau chế tạo, optional, mặc định rỗng — vd. gàu nước để lại gàu không).

### `shapeless` — không theo khuôn

```json
{
  "type": "shapeless",
  "id": "gunpowder_mix",
  "result": { "item": "minecraft:tnt", "count": 1 },
  "ingredients": ["minecraft:gunpowder", "minecraft:gunpowder", "minecraft:sand"]
}
```

`ingredients` (mảng `CraftingIngredient`, bắt buộc) — trùng lặp trong mảng là **yêu cầu riêng biệt**, không tự gộp số lượng (2 phần tử `"minecraft:gunpowder"` nghĩa là cần 2 cục thuốc súng, không phải 1 cục với count ẩn).

### `fusion` — bàn thờ nâng cấp, 1 slot trung tâm + nguyên liệu bao quanh

```json
{
  "type": "fusion",
  "id": "upgrade_fire_sword",
  "result": { "item": "fire_sword_plus", "count": 1 },
  "input": { "item": "fire_sword", "count": 1 },
  "ingredients": [ { "item": "minecraft:nether_star", "count": 1 } ]
}
```

`input` (`CraftingIngredient`, bắt buộc — vật phẩm trung tâm cần nâng cấp), `ingredients` (mảng `CraftingIngredient`, bắt buộc — nguyên liệu bao quanh). Khớp chính xác, không phải "ít nhất."

## `scrapping` — đã có class, **chưa đăng ký, chưa dùng được**

`ScrappingRecipe` (`input` + `outputs`, không có `result`) tồn tại trong code với `CODEC` hoạt động, nhưng **cố tình chưa được thêm vào `CraftingRecipes.java`** — doc comment của chính class này ghi rõ "chưa đăng ký vào `CraftingRecipes`/`CraftingManager`." Đừng viết `"type": "scrapping"` vào 1 file JSON thật, nó sẽ fail decode vì `Registries.CRAFTING_RECIPE_CODEC.getOrThrow("scrapping")` không tìm thấy gì.

## Cách thêm 1 loại công thức mới

1. Tạo class implement `CraftingRecipe`, khai `public static final Codec<YourRecipe> CODEC` (gộp `CraftingRecipe.BASE_CODEC` cho `id`/`result`, tương tự cách `SmpEffect.BASE_CODEC` được dùng — xem [Codec System](Codec-System.md#giả-lập-kế-thừa-bằng-1-mapcodec-field-chung)).
2. Đăng ký vào [`CraftingRecipes.java`](../src/main/java/com/roguesmp/crafting/recipe/CraftingRecipes.java).
3. Dùng `"type": "your_id"` trong `crafting_recipes/*.json`.
4. Nếu công thức mới cần 1 GUI riêng để hiển thị/chế tạo, viết 1 `ReactiveGui<S>` mới (xem [GUI Framework](GUI-Framework.md)) đọc từ `CraftingManager`.

## Lưu ý & lỗi thường gặp

- **`scrapping` tồn tại trong code nhưng chưa hoạt động** — đừng dùng nó trong nội dung thật cho đến khi nó xuất hiện trong `CraftingRecipes.java`.
- **`shapeless.ingredients` không gộp số lượng trùng lặp** — viết đúng số phần tử bằng đúng số lượng nguyên liệu cần, không viết 1 phần tử với hy vọng nó tự hiểu là "cần nhiều hơn 1."
- **`fusion` khớp chính xác** (không phải "chứa ít nhất") — nếu người chơi bỏ dư nguyên liệu ngoài danh sách `ingredients`, công thức sẽ không khớp.

---
◀ [Loot System](Loot-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Block System](Block-System.md)
