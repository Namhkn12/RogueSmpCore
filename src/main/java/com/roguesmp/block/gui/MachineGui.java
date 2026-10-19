package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.SlotGroup;
import com.roguesmp.block.transfer.SideConfig;
import com.roguesmp.block.transfer.SideMode;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A display-only view over a block's {@link ItemStorage}: every click is turned into a storage
 * call and the GUI is redrawn from the storage afterwards. Players can always take items out of a
 * bound slot, but only put items in where the group's access allows inserting.
 */
public abstract class MachineGui extends BaseGui {

    protected final ItemStorage storage;
    private final Consumer<HumanEntity> reopen;

    protected MachineGui(Component name, int rows, ItemStorage storage, Consumer<HumanEntity> reopen) {
        super(name, rows);
        this.storage = storage;
        this.reopen = reopen;
    }

    public abstract void refresh();

    protected abstract void onAllViewersClosed();

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);
        refresh();
    }

    protected void bindSlot(int guiSlot, SlotGroup group, int index, ItemStack placeholder) {
        StoredItem stored = group.get(index);
        ItemStack stack = stored == null ? null : stored.toItemStack();
        bindSlotDisplay(guiSlot, group, index, stack != null ? stack : placeholder);
    }

    protected void bindSlotDisplay(int guiSlot, SlotGroup group, int index, ItemStack display) {
        if (storage.isConfigurable()) appendTransferLore(display, group);
        addButton(guiSlot, display, event -> onSlotClick(event, group, index));
    }

    private void appendTransferLore(ItemStack display, SlotGroup group) {
        List<Component> lore = new ArrayList<>(display.lore() != null ? display.lore() : List.of());
        if (group.access().allowsInsert()) {
            lore.add(Utils.text("↓ Nhận từ: ", NamedTextColor.AQUA).append(Component.text(faceNames(storage.facesWhere(SideMode::pulls)), NamedTextColor.GRAY)));
        }
        if (group.access().allowsExtract()) {
            lore.add(Utils.text("↑ Đẩy ra: ", NamedTextColor.GOLD).append(Component.text(faceNames(storage.facesWhere(SideMode::pushes)), NamedTextColor.GRAY)));
        }
        ItemStackUtils.setLore(display, lore);
    }

    private static String faceNames(List<BlockFace> faces) {
        return faces.isEmpty() ? "chưa có mặt nào" : String.join(", ", faces.stream().map(SideConfig::displayName).toList());
    }

    protected void addSidesButton(int slot) {
        if (!storage.isConfigurable()) return;

        ItemStack item = ItemStack.of(Material.HOPPER);
        ItemStackUtils.setItemName(item, Utils.text("Cấu hình các mặt", NamedTextColor.AQUA));
        ItemStackUtils.setLore(item, List.of(Utils.text("Kéo/đẩy vật phẩm với khối bên cạnh", NamedTextColor.GRAY)));
        addButton(slot, item, event -> {
            event.setCancelled(true);
            HumanEntity viewer = event.getWhoClicked();
            Utils.runLater(() -> new SideConfigGui(storage, reopen).showInventory(viewer));
        });
    }

    private void onSlotClick(InventoryClickEvent event, SlotGroup group, int index) {
        event.setCancelled(true);
        StoredItem stored = group.get(index);

        if (event.isShiftClick()) {
            if (stored != null) giveTaken((Player) event.getWhoClicked(), group.extract(index, stored.amount(), false));
            return;
        }

        ItemStack cursor = event.getView().getCursor();
        if (cursor.getType().isAir()) {
            if (stored == null) return;
            int amount = event.isRightClick() ? (stored.amount() + 1) / 2 : stored.amount();
            setCursor(event, group.extract(index, amount, false));
            return;
        }

        StoredItem held = StoredItem.of(cursor);
        if (held == null) return;

        if (stored != null && held.item().equals(stored.item()) && !canPlayerInsert(group, held)) {
            int room = held.maxStackSize() - held.amount();
            StoredItem taken = group.extract(index, Math.min(room, stored.amount()), false);
            if (taken == null) return;
            ItemStack grown = cursor.clone();
            grown.setAmount(cursor.getAmount() + taken.amount());
            event.getView().setCursor(grown);
            return;
        }

        if (!canPlayerInsert(group, held)) return;

        if (stored != null && !held.item().equals(stored.item())) {
            StoredItem previous = group.extract(index, stored.amount(), false);
            group.insert(index, held, false);
            setCursor(event, previous);
            return;
        }

        StoredItem offered = event.isRightClick() ? held.withAmount(1) : held;
        int accepted = group.insert(index, offered, false);
        if (accepted > 0) event.getView().setCursor(shrink(cursor, accepted));
    }

    private boolean canPlayerInsert(SlotGroup group, StoredItem item) {
        return group.access().allowsInsert() && group.accepts(item);
    }

    private void giveTaken(Player player, @Nullable StoredItem taken) {
        ItemStack stack = taken == null ? null : taken.toItemStack();
        if (stack != null) PlayerUtils.giveItem(player, stack);
    }

    private void setCursor(InventoryClickEvent event, @Nullable StoredItem taken) {
        event.getView().setCursor(taken == null ? null : taken.toItemStack());
    }

    private static @Nullable ItemStack shrink(ItemStack stack, int amount) {
        if (stack.getAmount() <= amount) return null;
        ItemStack remaining = stack.clone();
        remaining.setAmount(stack.getAmount() - amount);
        return remaining;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (!event.isShiftClick()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        StoredItem stored = StoredItem.of(clicked);
        if (stored == null) return;

        int accepted = storage.insert(stored, false);
        if (accepted > 0) event.setCurrentItem(shrink(clicked, accepted));
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Utils.runLater(() -> {
            if (getInventory().getViewers().isEmpty()) onAllViewersClosed();
        });
    }

    public void closeAll() {
        for (HumanEntity viewer : List.copyOf(getInventory().getViewers())) viewer.closeInventory();
    }
}
