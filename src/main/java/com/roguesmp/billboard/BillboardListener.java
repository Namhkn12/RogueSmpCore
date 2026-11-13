package com.roguesmp.billboard;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BillboardListener implements Listener {

    private final BillboardManager manager;

    public BillboardListener(BillboardManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMarkerAdd(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof Marker marker) || BillboardManager.placedId(marker) == null) return;
        Bukkit.getScheduler().runTask(RogueSmpCore.getInstance(), () -> {
            if (marker.isValid()) manager.attach(marker);
        });
    }

    @EventHandler
    public void onMarkerRemove(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof Marker marker) || BillboardManager.placedId(marker) == null) return;
        Bukkit.getScheduler().runTask(RogueSmpCore.getInstance(), () -> {
            if (!marker.isValid()) manager.detach(marker.getUniqueId());
        });
    }

    @EventHandler
    public void onUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        if (!event.isAttack() && event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        int entityId = event.getEntityId();
        boolean rightClick = !event.isAttack();
        if (Bukkit.isPrimaryThread()) manager.click(player, entityId, rightClick);
        else Bukkit.getScheduler().runTask(RogueSmpCore.getInstance(), () -> manager.click(player, entityId, rightClick));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }
}
