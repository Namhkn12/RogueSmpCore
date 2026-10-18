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

## Ví dụ 1 — đơn giản: `IslandLeaveConfirmGui`

[`gui/island/IslandLeaveConfirmGui.java`](../src/main/java/com/roguesmp/gui/island/IslandLeaveConfirmGui.java) — 1 hộp thoại xác nhận 3 hàng:

```java
public class IslandLeaveConfirmGui extends BaseGui {
    public IslandLeaveConfirmGui(UUID islandId, Player player) {
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

## Ví dụ 2 — slot người chơi tự đặt item: `scheduleRefresh()`

GUI cho phép người chơi đặt item thẳng vào slot (bàn chế tạo, fusion) không đọc được nội dung slot ngay trong handler: `InventoryClickEvent`/`InventoryDragEvent` chạy **trước** khi server di chuyển item. `BaseGui` có sẵn hook trễ 1 tick:

```java
protected void scheduleRefresh();        // lên lịch onContentsSettled() ở tick sau, tối đa 1 lần/tick
protected void onContentsSettled() {}    // override: đọc lại slot, khớp công thức, vẽ lại
```

Chỉ gọi `scheduleRefresh()` khi thao tác **có thể** đổi slot người chơi đặt: click (không bị cancel) vào chính các slot đó — mọi kiểu click, kể cả phím số/F/Q; shift-click ở inventory dưới; drag có `getRawSlots()` chạm các slot đó; và sau khi GUI tự tiêu nguyên liệu. Click vào filler/nút hay click thường ở inventory dưới thì bỏ qua. Double-click bị `GuiListener` cancel toàn cục. Ví dụ: [`gui/crafting/CraftingGui.java`](../src/main/java/com/roguesmp/gui/crafting/CraftingGui.java), [`gui/crafting/FusionGui.java`](../src/main/java/com/roguesmp/gui/crafting/FusionGui.java).

GUI máy (`block/gui/MachineGui`) đi hướng khác: cancel mọi click và tự gọi storage, nên không cần trễ tick.

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
5. Với GUI có slot người chơi tự đặt item, gọi `scheduleRefresh()` từ các handler liên quan và override `onContentsSettled()` (xem Ví dụ 2).

## Lưu ý & lỗi thường gặp

- **Không bao giờ đăng ký thêm 1 listener `InventoryClickEvent` thô cho GUI tùy chỉnh.** `GuiListener` là điểm điều phối duy nhất; thêm listener khác có nguy cơ xử lý trùng lặp hoặc tranh nhau gọi `setCancelled`.
- **`fillEmpty()` gán 1 handler chỉ-để-cancel** cho mọi slot nó lấp — nếu gọi `addItem` (chỉ item, không handler) lên 1 slot *sau* `fillEmpty()`, click vào slot đó sẽ không làm gì cả vì giờ nó không có handler nào (dùng `addButton` nếu cần cả 2).

---
◀ [Attribute System](Attribute-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Entity, Boss & Spell System](Entity-Boss-Spell-System.md)
