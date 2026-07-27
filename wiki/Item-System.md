# Item System

Package: [`com.roguesmp.item`](../src/main/java/com/roguesmp/item)

Mô hình 2 tầng: 1 template JSON bất biến (`BaseItem`) và 1 wrapper runtime sống trên từng `ItemStack` (`SmpItem`), được mở rộng thông qua interface hook kiểu plugin `ItemComponent`.

## `BaseItem` — template

[`BaseItem.java`](../src/main/java/com/roguesmp/item/BaseItem.java) là prototype bất biến, định nghĩa bằng JSON: `id`, `base` (Material), `unique` (boolean), và `Map<String, ItemComponent> components`. Nó thuộc sở hữu của [`Registries.ITEM`](Registry-System.md) và không bao giờ tự thay đổi — `generateItemStack(...)` chỉ đơn giản tạo 1 `new SmpItem(this)` tạm thời rồi ủy quyền:

```java
public static final Codec<BaseItem> CODEC = Codec.composite(
        Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
        Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
        Codec.BOOLEAN.optionalFieldOf("unique", false).forGetter(BaseItem::isUnique),
        Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                .optionalFieldOf("components", Map.of())
                .forGetter(BaseItem::getComponents),
        BaseItem::new
);
```

Xem [Codec System → ví dụ BaseItem](Codec-System.md#full-worked-example--baseitem-dispatch--dispatchedmap--registry-loading) để biết `dispatchedMap` ở đây tra codec của từng component như thế nào, và [Registry System](Registry-System.md) để biết các file `*.json` dưới `plugin.getDataFolder()/items/` được decode vào `Registries.ITEM` lúc khởi động ra sao.

## `SmpItem` — wrapper sống

[`SmpItem.java`](../src/main/java/com/roguesmp/item/SmpItem.java) bọc quanh 1 `ItemStack` thật. **Không bao giờ tự tạo 1 instance cho 1 item đã tồn tại trong game** — luôn đi qua cache:

```java
SmpItem item = SmpItem.wrap(itemStack);                          // không có context player
SmpItem item = SmpItem.wrap(itemStack, smpPlayer);                // có context player
SmpItem item = SmpItem.wrap(itemStack, smpPlayer, onCacheMiss);   // + callback lúc wrap lần đầu
```

Cả 3 đều ủy quyền cho [`ItemManager`](../src/main/java/com/roguesmp/item/ItemManager.java) (singleton, `getInstance()`), class này giữ 1 cache Caffeine (`expireAfterAccess(5s)`, `expireAfterWrite(20s)`) khóa theo UUID của item (lấy từ PDC). Item không unique (không có UUID ổn định) không thể cache được — `wrapItem` tạo 1 `SmpItem` tạm thời mới cho mỗi lần gọi với những item này.

Tự khởi tạo trực tiếp chỉ dành cho 2 trường hợp mà chính `SmpItem` cần: `new SmpItem(ItemStack)` tái tạo lại từ PDC của 1 stack đã có sẵn (đọc `Keys.ITEM_ID`/`Keys.ITEM_UUID`, tra `BaseItem` qua `ItemRegistry`), và `new SmpItem(BaseItem)` tạo 1 item hoàn toàn mới từ template (sinh UUID nếu `unique`).

### Pipeline lắp ráp `generateItemStack`

Đây là method biến dữ liệu component thành 1 `ItemStack` thật sự được render — nên đọc kỹ trước khi viết 1 component mới, vì nó quyết định *thời điểm* các hook của bạn chạy so với nhau:

```java
public ItemStack generateItemStack(@Nullable SmpPlayer player, int stackAmount) {
    if (baseItem == null) return itemStack;

    ItemStack result = ItemStack.of(baseItem.getBase(), stackAmount);
    // dọn bớt tooltip vanilla, ép unbreakable, ẩn hiển thị attribute/enchant mặc định
    result.setData(DataComponentTypes.TOOLTIP_DISPLAY, ...);
    result.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
    result.setData(DataComponentTypes.UNBREAKABLE);

    PersistentDataContainerView oldData = itemStack.getPersistentDataContainer();
    LoreBuilder loreBuilder = new LoreBuilder();

    result.editPersistentDataContainer(pdc -> {
        oldData.copyTo(pdc, true);
        pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, baseItem.getId());
        ItemDataContext dataContext = new ItemDataContext(this, player, result, pdc);
        ItemLoreContext loreContext = new ItemLoreContext(this, player, loreBuilder, pdc);

        applyModifiers(player);   // 1. các modifier xuyên component chạy trước (gem, enchant, ...)

        for (Map.Entry<String, ItemComponent> entry : componentMap.entrySet()) {
            ItemComponent itemComponent = entry.getValue();
            itemComponent.save(pdc);              // 2. lưu state runtime có thể thay đổi
            itemComponent.modifyStack(dataContext); // 3. đụng vào data component vanilla
            itemComponent.contributeLore(loreContext); // 4. đẩy dòng lore
        }
    });

    result.setData(DataComponentTypes.LORE, ItemLore.lore(loreBuilder.build()));
    this.itemStack = result;
    return result;
}
```

Thứ tự: **tạo stack gốc → dọn data component vanilla → copy PDC cũ sang + gắn `ITEM_ID` → `applyModifiers(player)` → với mỗi component: `save` → `modifyStack` → `contributeLore` → ghi lore đã gom lại.**

`applyModifiers` chạy *trước* vòng lặp component để 1 [`ItemModifier`](../src/main/java/com/roguesmp/item/modifier/ItemModifier.java) (vd. `EnchantAttributeModifier` đọc `EnchantComponent` rồi đẩy giá trị vào `EquipAttributeComponent` qua `putModifier(...)`) có thể thay đổi state của component mà vòng lặp sau đó sẽ render/lưu lại. Các modifier được sắp thứ tự trong [`ModifierRegistry`](../src/main/java/com/roguesmp/registry/ModifierRegistry.java) (`GemModifier`, `EnchantAttributeModifier`, `BrokenModifier`) và chỉ chạy 1 lần cho mỗi instance `SmpItem` (nhờ cờ `loadedModifiers`).

`loadData(pdc)` là phần đối xứng, chạy 1 lần khi 1 `SmpItem` được tạo — với mỗi component trên `BaseItem`, nó gọi `copy()` để lấy 1 bản copy mutable riêng cho instance, rồi `load(pdc)` để hydrate state runtime:

```java
private void loadData(PersistentDataContainerView pdc) {
    for (Map.Entry<String, ItemComponent> entry : baseItem.getComponents().entrySet()) {
        ItemComponent copy = entry.getValue().copy();
        componentMap.put(entry.getKey(), copy);
        copy.load(pdc);
    }
}
```

## `ItemComponent` — điểm mở rộng

[`ItemComponent.java`](../src/main/java/com/roguesmp/item/component/ItemComponent.java), toàn bộ interface:

```java
public interface ItemComponent {
    /** Copy BaseItem component into active SmpItem instance */
    @NotNull ItemComponent copy();

    default void contributeLore(ItemLoreContext context) { }
    /** For modifying vanilla ItemStack stuff */
    default void modifyStack(ItemDataContext context) { }
    /** Load data from pdc */
    default void load(PersistentDataContainerView pdc) { }
    /** Save data to pdc */
    default void save(PersistentDataContainer pdc) { }
}
```

| Hook | Bắt buộc? | Được gọi khi | Dùng để |
| :--- | :--- | :--- | :--- |
| `copy()` | **có** | 1 lần, trong `SmpItem.loadData`, biến component dùng chung của `BaseItem` thành 1 bản copy mutable riêng cho instance | Với component không state: `return this;`. Với component mutable: `return new X(...)`. |
| `load(pdc)` | không | 1 lần, ngay sau `copy()`, trong `SmpItem.loadData` | Hydrate state runtime mutable (vd. độ bền hiện tại) từ PDC sẵn có của item. |
| `save(pdc)` | không | Mỗi lần gọi `generateItemStack`, đầu tiên trong vòng lặp component | Lưu state runtime mutable vào PDC của stack mới, trước khi `modifyStack`/`contributeLore` chạy. |
| `modifyStack(context)` | không | Mỗi lần gọi `generateItemStack`, sau `save` | Thay đổi `DataComponentTypes` vanilla trên `context.newStack()` (tên, max stack size, nội dung potion, ...). |
| `contributeLore(context)` | không | Mỗi lần gọi `generateItemStack`, cuối cùng | Đẩy dòng lore vào `LoreBuilder` dùng chung qua `context.builder().putLines(priority, lines)`. |

## `ComponentKey` và `ComponentKeys` — định danh & đăng ký

`ComponentKey<T extends ItemComponent>` ([`ComponentKey.java`](../src/main/java/com/roguesmp/item/component/ComponentKey.java)) chỉ đơn giản là `record ComponentKey<T>(String id)` — 1 handle đã gõ kiểu, dùng làm key trong map component của `BaseItem`/`SmpItem` (qua `.id()`), và làm tham số cho `baseItem.getComponent(key)` / `smpItem.getComponent(key)`.

[`ComponentKeys.java`](../src/main/java/com/roguesmp/constant/ComponentKeys.java) khai báo mọi hằng số `ComponentKey<T>`, và trong static initializer, đăng ký `Codec` của từng component cùng lúc:

```java
public static final ComponentKey<DurabilityComponent> DURABILITY;
static {
    DURABILITY = ItemComponentCodecRegistry.register("durability", DurabilityComponent.CODEC);
    ...
    BROKEN = new ComponentKey<>("broken"); // Transient so it has no CODEC.
}

public static void loadClass() { }
```

`ItemComponentCodecRegistry.register(id, codec)` vừa tạo key vừa lưu codec vào [`Registries.ITEM_COMPONENT_CODEC`](Registry-System.md) — bảng tra mà `dispatchedMap` của `BaseItem.CODEC` dùng khi decode 1 object `"components"`. `BROKEN` là ngoại lệ duy nhất: transient/chỉ tồn tại runtime, nên được tạo trực tiếp mà không có codec nào cả.

`loadClass()` là 1 no-op cố tình để trống — công việc duy nhất của nó là *được gọi* (`RogueSmpCore.init()`, trước khi bất kỳ thứ gì decode JSON của item) để ép static initializer của class này chạy. Java load class theo kiểu lazy, nên nếu không có lệnh gọi này, việc đăng ký codec component có thể bị trễ đến lần dùng đầu tiên — **gọi nó, đừng xóa nó.**

## Hai ví dụ minh họa

### Đơn giản — `StackSizeComponent` (không state, không PDC)

```java
public record StackSizeComponent(int size) implements ItemComponent {
    public static final Codec<StackSizeComponent> CODEC = Codec.INT.xmap(StackSizeComponent::new, StackSizeComponent::size);

    @Override public @NotNull ItemComponent copy() { return this; } // không có state mutable, chia sẻ an toàn

    @Override public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.MAX_STACK_SIZE, size);
    }
}
```

### Phức tạp — `DurabilityComponent` (giá trị template bất biến + state runtime mutable backing bởi PDC)

```java
public final class DurabilityComponent implements ItemComponent {
    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(Keys.GLOBAL_NAMESPACE, "durability");
    public static final Codec<DurabilityComponent> CODEC = Codec.INT.xmap(DurabilityComponent::new, DurabilityComponent::maxDurability);

    private final int maxDurability;       // từ JSON, dùng chung/bất biến
    @GsonIgnore
    private int currentDurability;         // chỉ tồn tại runtime, KHÔNG được round-trip qua JSON của BaseItem

    public DurabilityComponent(int maxDurability) {
        this.maxDurability = maxDurability;
        this.currentDurability = maxDurability;
    }

    @Override public @NotNull ItemComponent copy() { return new DurabilityComponent(maxDurability); }

    @Override public void load(PersistentDataContainerView pdc) {
        Integer current = pdc.get(DURABILITY_KEY, PersistentDataType.INTEGER);
        this.currentDurability = current == null ? maxDurability : current;
    }

    @Override public void save(PersistentDataContainer pdc) {
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, currentDurability);
    }

    @Override public void contributeLore(ItemLoreContext context) {
        // ... dựng 1 dòng lore "Độ bền: X/Y" có màu, dựa trên currentDurability/maxDurability
        context.builder().putLines(110, List.of(Component.empty(), loreComponent));
    }
}
```

`@GsonIgnore` đánh dấu `currentDurability` bị loại khỏi quá trình round-trip JSON của `BaseItem` — đây là state riêng cho từng instance, chỉ sống trong PDC của 1 `ItemStack`, được hydrate qua `load`/`save`.

## Cách thêm 1 `ItemComponent` mới

1. Tạo 1 class trong `item/component/impl/` implement `ItemComponent`. Khai `public static final Codec<YourComponent> CODEC` — `Codec.INT.xmap(...)`/tương tự cho 1 giá trị đơn, `Codec.composite(...)` cho nhiều field có tên (xem [Codec System](Codec-System.md)).
2. Implement `copy()` (bắt buộc). Chỉ thêm `load`/`save` nếu có state runtime mutable cần lưu vào PDC (định nghĩa 1 `NamespacedKey` dưới `Keys.GLOBAL_NAMESPACE`, theo mẫu `DurabilityComponent.DURABILITY_KEY`). Thêm `modifyStack` để đụng vào `DataComponentTypes` vanilla. Thêm `contributeLore` để đóng góp dòng lore.
3. Đăng ký nó trong [`ComponentKeys.java`](../src/main/java/com/roguesmp/constant/ComponentKeys.java): khai `public static final ComponentKey<YourComponent> YOUR_KEY;` và trong static block, `YOUR_KEY = ItemComponentCodecRegistry.register("your_id", YourComponent.CODEC);`.
4. Không cần nối gì thêm — `ComponentKeys.loadClass()` đã chạy sẵn lúc plugin `init()`.
5. Tham chiếu `"your_id"` trong object `"components"` của 1 item ở `plugins/.../items/*.json`; `BaseItem.CODEC` sẽ tự động decode nó.
6. Truy cập nó ở nơi khác qua `baseItem.getComponent(ComponentKeys.YOUR_KEY)` / `smpItem.getComponent(ComponentKeys.YOUR_KEY)`.

## `ItemModifier` — logic xuyên component

[`ItemModifier.java`](../src/main/java/com/roguesmp/item/modifier/ItemModifier.java): interface 1 method `void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player)`. Dùng cho hành vi trải rộng trên *nhiều* component — vd. gem/enchant đẩy giá trị dẫn xuất vào `EquipAttributeComponent`, hoặc state của `BrokenComponent` phụ thuộc vào `DurabilityComponent`. Được đăng ký theo thứ tự cố định trong `ModifierRegistry` và luôn chạy 1 lần, ở đầu `SmpItem.generateItemStack`, trước khi bất kỳ hook component nào chạy.

## Lưu ý & lỗi thường gặp

- **Không bao giờ `new SmpItem(itemStack)` cho 1 item đã có trong túi đồ/thế giới của player** — dùng `SmpItem.wrap(...)` để tận dụng cache của `ItemManager` và không chạy lại `applyModifiers` 1 cách không cần thiết.
- **Đánh dấu `@GsonIgnore` cho mọi field chỉ tồn tại trong PDC/runtime.** Nếu nó vô tình round-trip qua JSON của `BaseItem`, dữ liệu template và state sống sẽ bị lẫn vào nhau.
- **Thứ tự hook của component là cố định** (`save` → `modifyStack` → `contributeLore`, sau `applyModifiers`) — nếu lore của 1 component phụ thuộc vào state mà `modifyStack` của component khác thiết lập, sự phụ thuộc đó sẽ không thấy được; hãy dùng 1 `ItemModifier` cho các mối quan tâm về thứ tự xuyên component.
- **`ItemRegistry.getInstance()` cũ đã bị `@Deprecated`** — xem [Registry System](Registry-System.md#the-migration-registry-getinstance--registries); dùng `Registries.ITEM` trong code mới.

---
◀ [Registry System](Registry-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Attribute System](Attribute-System.md)
