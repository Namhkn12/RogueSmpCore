package com.roguesmp.item.interaction;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * For special function of an item (right click to receive money, for example)
 */
public interface ItemInteraction {

    default void onInteract(PlayerInteractEvent event) {

    }

    default void onClickInventory(InventoryClickEvent event) {

    }
}
