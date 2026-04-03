package com.roguesmp.listener;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.interaction.ItemInteraction;
import com.roguesmp.registry.ItemInteractionRegistry;
import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemInteractionListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        String itemId = ItemStackUtils.getId(itemStack);
        if (itemId == null) return;
        ItemInteraction interaction = ItemInteractionRegistry.getInteraction(itemId);
        if (interaction == null) return;
        interaction.onInteract(event);
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        // Don't run if player has a custom gui open
        if (!(topInventory.getHolder(false) instanceof BaseGui)) return;
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;
        ItemStack itemStack = clickedInventory.getItem(event.getSlot());
        if (itemStack == null) return;
        String itemId = ItemStackUtils.getId(itemStack);
        if (itemId == null) return;
        ItemInteraction interaction = ItemInteractionRegistry.getInteraction(itemId);
        if (interaction == null) return;
        interaction.onClickInventory(event);
    }
}
