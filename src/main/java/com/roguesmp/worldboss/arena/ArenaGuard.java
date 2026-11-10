package com.roguesmp.worldboss.arena;

import com.roguesmp.utils.Utils;
import com.roguesmp.worldboss.WorldBossManager;
import com.roguesmp.worldboss.runtime.RemovalReason;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ArenaGuard implements Listener {

    private final WorldBossManager manager;

    public ArenaGuard(WorldBossManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        WorldBossInstance fight = manager.getBlockingFight(player, event.getTo());
        if (fight == null || fight.contains(event.getFrom())) return;

        event.setCancelled(true);
        player.teleport(manager.exitFor(fight.getDefinition(), player), PlayerTeleportEvent.TeleportCause.PLUGIN);
        warn(player, fight);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        WorldBossInstance fight = manager.getBlockingFight(player, event.getTo());
        if (fight == null || fight.contains(event.getFrom())) return;

        event.setCancelled(true);
        warn(player, fight);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        manager.eliminate(event.getPlayer(), RemovalReason.DEATH);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.eliminate(event.getPlayer(), RemovalReason.LOGOUT);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location exit = manager.takePendingRespawn(player);
        if (exit == null) {
            WorldBossInstance fight = manager.getBlockingFight(player, event.getRespawnLocation());
            if (fight == null) return;
            exit = manager.exitFor(fight.getDefinition(), player);
        }
        event.setRespawnLocation(exit);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        WorldBossInstance fight = manager.getBlockingFight(player, player.getLocation());
        if (fight == null) return;
        player.teleport(manager.exitFor(fight.getDefinition(), player), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        manager.handleEntityDeath(event.getEntity());
    }

    private void warn(Player player, WorldBossInstance fight) {
        player.sendActionBar(Utils.fromString("<red>Trận đấu với " + fight.getDefinition().displayName() + " <red>đang diễn ra."));
    }
}
