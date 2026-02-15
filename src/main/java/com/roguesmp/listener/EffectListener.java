package com.roguesmp.listener;

import com.roguesmp.effect.EffectManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EffectListener implements Listener {
    private final EffectManager effectManager;

    public EffectListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        effectManager.onEntityDeath(event);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        effectManager.onPlayerJoin(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        effectManager.onPlayerQuit(event);
    }
}
