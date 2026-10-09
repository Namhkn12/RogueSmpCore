package com.roguesmp.region.listener;

import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.region.event.RegionTransitionEvent;
import com.roguesmp.region.event.RegionTransitionEvent.Occupancy;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionListener implements Listener {

    private final ServerRegionManager serverRegionManager;
    private final Map<UUID, Occupancy> occupied = new HashMap<>();

    public RegionListener(ServerRegionManager serverRegionManager) {
        this.serverRegionManager = serverRegionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) update(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        update(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Occupancy previous = occupied.remove(player.getUniqueId());
        if (previous != null && !previous.regionIds().isEmpty()) {
            new RegionTransitionEvent(player, previous, Occupancy.empty(previous.world())).callEvent();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAbilityCast(AbilityCastEvent event) {
        Player player = event.getSmpPlayer().getBukkitPlayer();
        if (player == null) return;

        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        boolean allowed = query.testState(BukkitAdapter.adapt(player.getLocation()), WorldGuardPlugin.inst().wrapPlayer(player), SmpRegionFlags.ABILITIES);
        if (!allowed) event.setCancelled(true);
    }

    private void update(Player player, Location location) {
        Occupancy current = new Occupancy(location.getWorld(), serverRegionManager.regionIdsAt(location));
        Occupancy previous = occupied.put(player.getUniqueId(), current);
        if (previous == null) previous = Occupancy.empty(current.world());

        boolean sameWorld = previous.world().equals(current.world());
        if (sameWorld && previous.regionIds().equals(current.regionIds())) return;
        if (previous.regionIds().isEmpty() && current.regionIds().isEmpty()) return;

        new RegionTransitionEvent(player, previous, current).callEvent();
    }
}
