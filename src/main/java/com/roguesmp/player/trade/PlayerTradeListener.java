package com.roguesmp.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerTradeListener implements Listener {

    private final PlayerTradeManager tradeManager;

    public PlayerTradeListener(PlayerTradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShiftInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        if (!player.isSneaking() || target.hasMetadata("NPC")) return;

        event.setCancelled(true);
        tradeManager.handleShiftInteract(player, target);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tradeManager.handleQuit(event.getPlayer());
    }
}
