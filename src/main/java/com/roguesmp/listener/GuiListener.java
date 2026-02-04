package com.roguesmp.listener;

import com.roguesmp.gui.BaseGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {
    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof BaseGui baseGui)) return;
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            baseGui.onClickOutsideInventory(event);
            return;
        }
        if (clickedInventory.getType() == InventoryType.PLAYER) {
            baseGui.onClickBottomInventory(event);
        } else {
            baseGui.onClickTopInventory(event);
        }

    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof BaseGui baseGui)) return;
        baseGui.onDragInventory(event);
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof BaseGui baseGui)) return;
        baseGui.onCloseInventory(event);
    }

    @EventHandler
    public void onGuiOpen(InventoryOpenEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof BaseGui baseGui)) return;
        baseGui.onOpenInventory(event);
    }
}
