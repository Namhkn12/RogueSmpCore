package com.roguesmp.item.interaction;

import com.roguesmp.gui.WalletGui;
import org.bukkit.event.player.PlayerInteractEvent;

public class WalletInteraction implements ItemInteraction {
    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.hasItem()) {
            new WalletGui(event.getItem(), event.getPlayer()).showInventory(event.getPlayer());
        }
    }
}
