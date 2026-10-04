# Block System

Package: [`com.roguesmp.block`](../src/main/java/com/roguesmp/block)

Hệ block tùy chỉnh **vừa được viết lại gần đây nhất** trong toàn bộ codebase (`feat: Custom block rework`, `feat: Tickable interface for blocks`) — 1 block tùy chỉnh không phải block Bukkit thật, mà là 1 `ItemDisplay` entity đặt tại 1 tọa độ, theo dõi bởi `BlockManager`. Cùng tinh thần định nghĩa/hành vi tách rời như item/entity: `BlockProperties` là dữ liệu JSON thuần (không hành vi), `BlockType<? extends SmpBlock>` là hành vi code (không dữ liệu) — 2 nửa nối với nhau **bằng cách trùng id**, không phải 1 field `"type"` trong JSON.

## `BlockProperties` — dữ liệu JSON thuần, không có field `"type"`

[`block/BlockProperties.java`](../src/main/java/com/roguesmp/block/BlockProperties.java), registry `Registries.BLOCK_PROPERTIES = new Registry<>("blocks", BlockProperties.CODEC)`, thư mục `blocks/`. Mọi field optional, có default trong `BlockProperties.DEFAULT`:

| JSON key | Kiểu | Mặc định |
| :--- | :--- | :--- |
| `hardness` | int | `2` |
| `tools` | mảng tham chiếu [`Holder<ItemType>`](Registry-System.md#holdert--tham-chiếu-ổn-định-qua-id) — id trong `Registries.ITEM_TYPE` | `[]` |
| `break_strength` | int | `0` |
| `model` | Key (namespaced, resource-pack item model hiện trên display entity) | `"minecraft:stone"` |
| `drops` | mảng `BlockDrop` | `[]` |
| `experience` | `BlockExperience` | rỗng (không rơi kinh nghiệm) |
| `place_sound` | Key | `block.stone.place` |
| `break_sound` | Key | `block.stone.break` |

`BlockDrop`: `{ "item": "<key>", "min_amount": 1, "max_amount": 1, "chance": 1.0 }` (`min_amount`/`max_amount`/`chance` optional). `BlockExperience`: `{ "min_amount": 0, "max_amount": 0, "chance": 1.0 }` (tất cả optional).

```json
{
  "hardness": 5,
  "tools": ["pickaxe"],
  "break_strength": 3,
  "model": "smp:steel_block",
  "drops": [ { "item": "steel_ingot", "min_amount": 1, "max_amount": 3, "chance": 1.0 } ],
  "experience": { "min_amount": 1, "max_amount": 3, "chance": 0.5 },
  "place_sound": "block.metal.place",
  "break_sound": "block.metal.break"
}
```

## `BlockType<T extends SmpBlock>` — hành vi, đăng ký code, khớp bằng id file

**Không có field `"type"` trên `BlockProperties`** — hành vi được chọn hoàn toàn bằng cách khớp id. `Registry.loadFrom` derive id của 1 entry từ đường dẫn file (bỏ `.json`, xem [Registry System](Registry-System.md)), nên `blocks/steel_block.json` → id `"steel_block"`. `BlockType` (đăng ký in-memory, code-driven, trong [`BlockTypes.java`](../src/main/java/com/roguesmp/block/BlockTypes.java)) tự tra `Registries.BLOCK_PROPERTIES.getHolder(id)` **bằng đúng id đó**:

```java
public static final BlockType<SmpBlock> STEEL_BLOCK = register("steel_block", SmpBlock::new);
public static final BlockType<TallyBlock> TALLY_BLOCK = register("tally_block", TallyBlock::new);
```

Nghĩa là: muốn 1 block có hành vi riêng, id file JSON (`blocks/tally_block.json`) và id đăng ký `BlockType` (`"tally_block"`) **phải khớp tuyệt đối** — không khớp thì `BlockType` fallback dùng `BlockProperties.DEFAULT` (bị `validateAllHolders()` gắn cờ dạng tham chiếu treo nếu không có file nào khớp). 1 id chưa có `BlockType` riêng nào đăng ký thì đơn giản không đặt/spawn được như 1 loại block (khác với item/entity — không có "fallback hành vi rỗng" ở đây, vì `BlockType` chính là cách 1 block tồn tại).

## `SmpBlock` — hook hành vi (mirror `ItemComponent`/`EntityComponent`)

Hook chính, mọi hook đều override tùy chọn: `onPlaced(Player, ItemStack)`, `getDrops()` (mặc định roll theo `BlockProperties.drops()`), `getExperience()` (mặc định roll theo `BlockProperties.experience()`), `onBlockBreak(BlockBreakEvent)`, `onBlockInteract(PlayerInteractEvent)`, `onUnload()`, `onHydrated()`. State runtime cần lưu (persist) khai qua `collectSections(List<StateSection<?>>)`, đánh dấu `markDirty()` khi thay đổi. Identity 1 instance = `BlockType<?>` + `BlockPos` + 1 `UUID` display entity riêng (`BlockVisual` quản lý entity `ItemDisplay` đó).

### `Tickable` — mixin định kỳ

```java
public interface Tickable {
    void tick();
}
```

1 `SmpBlock` implement thêm `Tickable` sẽ được `BlockManager` tự thêm vào vòng tick mỗi-tick (opt-in, block không implement không tốn chi phí gì). **Hiện chưa có `SmpBlock` nào trong codebase implement interface này** — `TallyBlock` (ví dụ có sẵn duy nhất, tăng bộ đếm mỗi lần click phải, hiện trên action bar) chỉ phản ứng theo `onBlockInteract`, không tick.

## Cách thêm 1 loại block mới

1. Viết `blocks/<id>.json` (JSON thuần dữ liệu — hardness, drop, sound, model — không có gì về hành vi).
2. Nếu chỉ cần 1 block "trơ" (break/drop/exp theo JSON, không hành vi riêng): dùng thẳng `SmpBlock::new` làm factory, không cần class riêng.
3. Nếu cần hành vi riêng (đếm số lần click như `TallyBlock`, tick định kỳ, ...): viết class `extends SmpBlock` (implement thêm `Tickable` nếu cần tick), override đúng hook cần.
4. Đăng ký trong [`BlockTypes.java`](../src/main/java/com/roguesmp/block/BlockTypes.java): `public static final BlockType<YourBlock> YOUR_BLOCK = register("<id giống hệt tên file JSON>", YourBlock::new);`.

## Lưu ý & lỗi thường gặp

- **Không có field `"type"` để chọn hành vi** — khớp hoàn toàn bằng id (tên file = id đăng ký `BlockType`). Đổi tên file JSON mà quên đổi id đăng ký trong `BlockTypes.java` (hoặc ngược lại) sẽ âm thầm rơi về `BlockProperties.DEFAULT`.
- **`Tickable` là opt-in, không phải mặc định** — implement nó chỉ khi thực sự cần logic mỗi tick; đa số block (kể cả có hành vi click) không cần.
- Đây là subsystem **vừa rework xong gần nhất** — nếu thấy hành vi không khớp mô tả ở đây, ưu tiên đọc lại `SmpBlock`/`BlockManager`/`BlockVisual` trực tiếp trước khi giả định trang này vẫn đúng.

---
◀ [Crafting System](Crafting-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Fx System](Fx-System.md)
