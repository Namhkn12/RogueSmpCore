package com.roguesmp.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.roguesmp.npc.NpcManager;
import com.roguesmp.npc.SmpNpc;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NpcListener implements Listener {

    private final NpcManager manager;

    public NpcListener(NpcManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        SmpNpc npc = manager.wrap(event.getEntity());
        if (npc == null) return;
        manager.register(npc);
    }

    @EventHandler
    public void onRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        manager.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        SmpNpc npc = manager.getNpc(clicked);
        if (npc == null) return;
        npc.onRightClick(event);
    }

    @EventHandler
    public void onLeftClick(PrePlayerAttackEntityEvent event) {
        Entity attacked = event.getAttacked();
        SmpNpc npc = manager.getNpc(attacked);
        if (npc == null) return;
        event.setCancelled(true);
        npc.onLeftClick(event);
    }
}
