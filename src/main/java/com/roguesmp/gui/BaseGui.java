package com.roguesmp.gui;

import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseGui implements InventoryHolder {

    protected final ItemStack FILLER;
    protected final ItemStack FILLER_BLACK;
    protected final ItemStack NEXT_PAGE_BUTTON;
    protected final ItemStack PREV_PAGE_BUTTON;

    private final Map<Integer, ClickHandler> handlerMap = new HashMap<>();
    private final Inventory inventory;
    private final Component name;

    public BaseGui(Component name, int row) {
        this.name = name;
        inventory = Bukkit.createInventory(this, row * 9, this.name);

        FILLER = ItemStack.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        FILLER.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        FILLER_BLACK = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE);
        FILLER_BLACK.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        ItemStack nextPageHead = SkinRegistry.getInstance().getHead("gui_next_page");
        NEXT_PAGE_BUTTON = nextPageHead == null ? new ItemStack(Material.ARROW) : nextPageHead;
        NEXT_PAGE_BUTTON.setData(DataComponentTypes.CUSTOM_NAME, Utils.text("Trang sau »"));

        ItemStack prevPageHead = SkinRegistry.getInstance().getHead("gui_prev_page");
        PREV_PAGE_BUTTON = prevPageHead == null ? new ItemStack(Material.ARROW) : prevPageHead;
        PREV_PAGE_BUTTON.setData(DataComponentTypes.ITEM_NAME, Utils.text("« Trang trước"));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public abstract void setup();

    public void fillEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (ItemStackUtils.isValidItem(inventory.getItem(i))) continue;
            addButton(i, FILLER, event -> event.setCancelled(true));
        }
    }

    public void fillEmpty(ItemStack itemStack) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (ItemStackUtils.isValidItem(inventory.getItem(i))) continue;
            addButton(i, itemStack, event -> event.setCancelled(true));
        }
    }

    /**
     * Add an ItemStack to the slot and add/bind a ClickHandler to the same slot
     * @param slot THe slot
     * @param displayItem The ItemStack to show in inventory
     * @param handler The ClickHandler
     */
    public void addButton(int slot, ItemStack displayItem, ClickHandler handler) {
        inventory.setItem(slot, displayItem);
        handlerMap.put(slot, handler);
    }

    /**
     * Add/Bind an ClickHandler to this slot
     * @param slot The slot
     * @param clickHandler The handler
     */
    public void addAction(int slot, ClickHandler clickHandler) {
        handlerMap.put(slot, clickHandler);
    }

    /**
     * Set an itemStack in inventory without doing anything else
     * @param slot slot
     * @param itemStack the itemStack
     */
    public void addItem(int slot, @Nullable ItemStack itemStack) {
        inventory.setItem(slot, itemStack);
    }

    public void addButton(int row, int col, @Nullable ItemStack displayItem, ClickHandler handler) {
        int slot = getSlot(row, col);
        inventory.setItem(slot, displayItem);
        handlerMap.put(slot, handler);
    }

    public void addAction(int row, int col, ClickHandler clickHandler) {
        int slot = getSlot(row, col);
        handlerMap.put(slot, clickHandler);
    }

    public void addItem(int row, int col, @Nullable ItemStack itemStack) {
        int slot = getSlot(row, col);
        inventory.setItem(slot, itemStack);
    }

    public static ItemStack createDecoration(Material material) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.empty());
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }

    /**
     * Clear the inventory, including its button handler
     */
    public void clearUi() {
        handlerMap.clear();
        inventory.clear();
    }

    public void showInventory(Player player) {
        setup();
        player.openInventory(inventory);
    }

    public void showInventory(HumanEntity player) {
        setup();
        player.openInventory(inventory);
    }

    /**
     * Called when user click on our gui's inventory, used for processing buttons, override if needed more control.
     */
    public void onClickTopInventory(InventoryClickEvent event) {
        int slot = event.getSlot();
        ClickHandler handler = handlerMap.get(slot);
        if (handler == null) {
            return;
        }
        handler.onClick(event);
    }

    /**
     * Called when user click on their player inventory
     */
    public void onClickBottomInventory(InventoryClickEvent event) {

    }

    /**
     * Called when user drag itemStack, can be called from both top and bottom... Maybe buggy
     */
    public void onDragInventory(InventoryDragEvent event) {

    }

    /**
     * Called when user click on the outside area of the inventory window
     */
    public void onClickOutsideInventory(InventoryClickEvent event) {

    }

    /**
     * Called when opening the gui
     */
    public void onOpenInventory(InventoryOpenEvent event) {
    }

    /**
     * Called when closing the gui
     */
    public void onCloseInventory(InventoryCloseEvent event) {

    }

    /**
     * Calculate the slot from given row and column coordinate
     * @param row Row index, start from 0
     * @param col Column index, start from 0
     * @return Slot index correspond to the coordinate
     */
    public int getSlot(int row, int col) {
        return row * 9 + col;
    }

    /**
     * Calculate the row index from a given slot
     * @param slot The slot index
     * @return The row index (starting from 0)
     */
    public int getRow(int slot) {
        return slot / 9;
    }

    /**
     * Calculate the column index from a given slot
     * @param slot The slot index
     * @return The column index (starting from 0, 0-8)
     */
    public int getColumn(int slot) {
        return slot % 9;
    }

    @FunctionalInterface
    public interface ClickHandler {
        void onClick(InventoryClickEvent event);

        /**
         * No action ClickHandler
         */
        static ClickHandler noAction() {
            return event -> event.setCancelled(true);
        }

        static ClickHandler openGui(Inventory inventory) {
            return event -> {
                event.setCancelled(true);
                event.getWhoClicked().openInventory(inventory);
            };
        }

        static ClickHandler openGui(BaseGui gui) {
            return event -> {
                event.setCancelled(true);
                Utils.runLater(() -> gui.showInventory(event.getWhoClicked()));
            };
        }
    }
}
