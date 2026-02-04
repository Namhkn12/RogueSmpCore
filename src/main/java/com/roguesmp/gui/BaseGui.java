package com.roguesmp.gui;

import com.roguesmp.utils.ItemUtils;
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

import java.util.HashMap;
import java.util.Map;

public abstract class BaseGui implements InventoryHolder {

    protected static final ItemStack FILLER;

    static {
        FILLER = ItemStack.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        FILLER.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
    }

    private final Map<Integer, ClickHandler> handlerMap = new HashMap<>();
    private final Inventory inventory;
    private final Component name;
    private final int row;

    public BaseGui(Component name, int row) {
        this.name = name;
        this.row = row;
        inventory = Bukkit.createInventory(this, this.row * 9, this.name);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public abstract void setup();

    public void fillEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (ItemUtils.isValidItem(inventory.getItem(i))) continue;
            addButton(i, FILLER, event -> event.setCancelled(true));
        }
    }

    public void fillEmpty(ItemStack itemStack) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (ItemUtils.isValidItem(inventory.getItem(i))) continue;
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
     * Set an item in inventory without doing anything else
     * @param slot slot
     * @param itemStack the item
     */
    public void addItem(int slot, ItemStack itemStack) {
        inventory.setItem(slot, itemStack);
    }

    public void addButton(int row, int col, ItemStack displayItem, ClickHandler handler) {
        int slot = getSlot(row, col);
        inventory.setItem(slot, displayItem);
        handlerMap.put(slot, handler);
    }

    public void addAction(int row, int col, ClickHandler clickHandler) {
        int slot = getSlot(row, col);
        handlerMap.put(slot, clickHandler);
    }

    public void addItem(int row, int col, ItemStack itemStack) {
        int slot = getSlot(row, col);
        inventory.setItem(slot, itemStack);
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
     * Called when user click on our gui's inventory
     */
    public void onClickTopInventory(InventoryClickEvent event) {
        int slot = event.getSlot();
        ClickHandler handler = handlerMap.get(slot);
        if (handler == null) {
            this.onClick(event);
            return;
        }
        handler.onClick(event);
    }

    /**
     * Override this if normal button function is not enough, there should be no ClickHandler bound to the clicked slot
     * @param event The event
     */
    public void onClick(InventoryClickEvent event) {

    }

    /**
     * Called when user click on their player inventory
     */
    public void onClickBottomInventory(InventoryClickEvent event) {

    }

    /**
     * Called when user drag item, can be called from both top and bottom... Maybe buggy
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
