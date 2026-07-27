# GUI Framework

Package: [`com.roguesmp.gui`](../src/main/java/com/roguesmp/gui)

Mọi GUI đều extend `BaseGui`, class này bọc 1 `Inventory` thật của Bukkit đằng sau `InventoryHolder` cộng với 1 `Map<Integer, ClickHandler>`. Việc điều phối click được tập trung ở 1 chỗ duy nhất — **không tự đăng ký thêm listener `InventoryClickEvent` cho từng GUI riêng**, mà hãy hook vào các method có thể override của `BaseGui`.

## `BaseGui`

[`BaseGui.java`](../src/main/java/com/roguesmp/gui/BaseGui.java) — class abstract, `implements InventoryHolder`.

```java
public BaseGui(Component name, int row) // tạo Inventory qua Bukkit.createInventory(this, row*9, name)

public abstract void setup(); // subclass đổ nội dung vào inventory ở đây
```

Đổ nội dung vào slot:

| Method | Có gán item? | Có gán handler? |
| :--- | :--- | :--- |
| `addButton(int slot, ItemStack item, ClickHandler handler)` (+ overload `(row, col, ...)`) | có | có |
| `addAction(int slot, ClickHandler handler)` (+ overload row/col) | không | có |
| `addItem(int slot, ItemStack item)` (+ overload row/col) | có | không |
| `fillEmpty()` / `fillEmpty(ItemStack)` | lấp item filler vào mọi slot còn trống | gán handler chỉ để cancel cho từng slot đó |

Các hook có thể override, được `GuiListener` điều phối tới:

```java
public void onClickTopInventory(InventoryClickEvent event)   // mặc định: điều phối tới ClickHandler của slot
public void onClickBottomInventory(InventoryClickEvent event) // mặc định: no-op
public void onClickOutsideInventory(InventoryClickEvent event) // mặc định: no-op
public void onDragInventory(InventoryDragEvent event)          // mặc định: no-op
public void onOpenInventory(InventoryOpenEvent event)          // mặc định: no-op
public void onCloseInventory(InventoryCloseEvent event)        // mặc định: no-op
```

Mở 1 GUI: gọi `showInventory(Player)` (hoặc `showInventory(HumanEntity)`) — nó chạy `setup()` rồi `player.openInventory(inventory)`. Đây là cách chuẩn để mở 1 GUI; 1 số GUI (xem `MachineGui` bên dưới) tự gọi `setup()` ngay trong constructor và được mở bằng `player.openInventory(gui.getInventory())` thô thay vì `showInventory`.

Các helper khác: `getSlot(row, col)`, `getRow(slot)`, `getColumn(slot)`, `clearUi()` (xóa sạch item + handler), `createDecoration(Material)`.

### `ClickHandler`

`@FunctionalInterface` lồng bên trong `BaseGui`:

```java
@FunctionalInterface
public interface ClickHandler {
    void onClick(InventoryClickEvent event);

    static ClickHandler noAction() { return event -> event.setCancelled(true); }
    static ClickHandler openGui(Inventory inventory) { ... }
    static ClickHandler openGui(BaseGui gui) { ... } // mở lại qua Utils.runLater
}
```

## `GuiListener` — điểm điều phối duy nhất

[`listener/GuiListener.java`](../src/main/java/com/roguesmp/listener/GuiListener.java) là *nơi duy nhất* xử lý `InventoryClickEvent`/`InventoryDragEvent`/`InventoryOpenEvent`/`InventoryCloseEvent` cho mọi GUI tùy chỉnh trong toàn plugin. Mọi handler đều bắt đầu bằng cùng 1 guard:

```java
if (!(event.getView().getTopInventory().getHolder(false) instanceof BaseGui baseGui)) return;
```

Handler click còn rẽ nhánh tiếp theo vị trí click: không có inventory bị click → `onClickOutsideInventory`; inventory bị click là của chính player (`InventoryType.PLAYER`) → `onClickBottomInventory`; còn lại → `onClickTopInventory`. Event drag/open/close chuyển thẳng tới hook `BaseGui` tương ứng.

## Mixin — `gui/interfaces/`

2 interface tùy chọn cho GUI dạng công thức/máy chế tạo:

```java
public interface IHaveBlueprint {
    void setBlueprintItem(BaseRecipe lockedRecipe);
    void setupBlueprintSlot();
}

public interface IHaveInputOutput {
    int[] getInputSlots();
    int[] getOutputSlots();
}
```

## Ví dụ 1 — đơn giản: `IslandLeaveConfirmGui`

[`gui/island/IslandLeaveConfirmGui.java`](../src/main/java/com/roguesmp/gui/island/IslandLeaveConfirmGui.java) — 1 hộp thoại xác nhận 3 hàng:

```java
public class IslandLeaveConfirmGui extends BaseGui {
    public IslandLeaveConfirmGui(Player player) {
        super(Component.text("Xác nhận rời đảo"), 3);
    }

    @Override
    public void setup() {
        clearUi();
        addButton(getSlot(1, 2), confirmItem, event -> { /* xóa thành viên đảo */ event.getWhoClicked().closeInventory(); });
        addButton(getSlot(1, 6), abortItem, ClickHandler.openGui(new IslandMainGui(player)));
        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
```

## Ví dụ 2 — phức tạp: `MachineGui`

[`gui/MachineGui.java`](../src/main/java/com/roguesmp/gui/MachineGui.java) (abstract) — `extends BaseGui implements IHaveInputOutput, IHaveBlueprint`. Các GUI máy cụ thể implement `getInputSlots()`, `getOutputSlots()`, `getProcessingSlot()`. `setup()` lấp decoration của máy, kiểm tra slot input/output nằm đúng phạm vi hàng, thêm nút cài đặt/duyệt công thức, rồi gọi `setupBlueprintSlot()` của chính nó:

```java
@Override
public void setupBlueprintSlot() {
    if (!(machine instanceof IHaveLockedRecipe)) return;
    // click phải: đọc PDC Keys.ITEM_ID của item trên con trỏ chuột, tra công thức qua
    // RecipeManager.findRecipeBasedOnMainOutput(...), khóa nó lại qua machine.setLockedRecipe(recipe)
    // click trái: mở khóa
}
```

Nó còn có `setEnergy(int, int)` / `setProcessing(ItemStack)`, cả 2 gọi `this.getInventory().setItem(...)` **trực tiếp** (không phải `addButton`) — đây là pattern để cập nhật trực tiếp 1 GUI đang mở, bỏ qua map handler vì hành vi click của slot không cần đổi, chỉ có item hiển thị cần đổi.

## Mở 1 GUI — call site thật

```java
new IslandMainGui(player).showInventory(player);   // ưu tiên: gọi setup() rồi mở
new TrashGui(3).showInventory(sender);

// Hoặc, khi setup() đã chạy sẵn trong constructor (vd. MachineGui):
event.getPlayer().openInventory(gui.getInventory());
```

## Cách tạo 1 GUI mới

1. Extend `BaseGui`; gọi `super(nameComponent, rows)` trong constructor.
2. Override `setup()`: đổ slot bằng `addButton`/`addAction`/`addItem`, kết thúc bằng `fillEmpty(...)`.
3. Chỉ override `onClickBottomInventory`/`onDragInventory`/v.v. nếu cần hành vi khác mặc định (đa số GUI chỉ cần cancel click ở inventory dưới để tránh xáo trộn item).
4. Khởi tạo và gọi `gui.showInventory(player)`.
5. Với GUI dạng máy/công thức có slot input/output và công thức có thể khóa, extend `MachineGui` thay vào đó và implement `getInputSlots()`, `getOutputSlots()`, `getProcessingSlot()`.

## Lưu ý & lỗi thường gặp

- **Không bao giờ đăng ký thêm 1 listener `InventoryClickEvent` thô cho GUI tùy chỉnh.** `GuiListener` là điểm điều phối duy nhất; thêm listener khác có nguy cơ xử lý trùng lặp hoặc tranh nhau gọi `setCancelled`.
- **`fillEmpty()` gán 1 handler chỉ-để-cancel** cho mọi slot nó lấp — nếu gọi `addItem` (chỉ item, không handler) lên 1 slot *sau* `fillEmpty()`, click vào slot đó sẽ không làm gì cả vì giờ nó không có handler nào (dùng `addButton` nếu cần cả 2).

---
◀ [Attribute System](Attribute-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Entity, Boss & Spell System](Entity-Boss-Spell-System.md)
