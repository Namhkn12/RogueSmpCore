package com.roguesmp.region.listener;

import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.event.RegionEvent;
import com.roguesmp.region.event.RegionLeaveEvent;
import com.roguesmp.tab.TabKeys;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RegionListener implements Listener {

    private final PlayerManager playerManager;

    public RegionListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onRegionEnter(RegionEnterEvent event) {
        updateTabRegionName(event);
    }

    @EventHandler
    public void onRegionLeave(RegionLeaveEvent event) {
        updateTabRegionName(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAbilityCast(AbilityCastEvent event) {
        Player player = event.getSmpPlayer().getBukkitPlayer();
        if (player == null) return;

        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        boolean allowed = query.testState(BukkitAdapter.adapt(player.getLocation()), WorldGuardPlugin.inst().wrapPlayer(player), SmpRegionFlags.ABILITIES);
        if (!allowed) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateTabRegionName(player, ServerRegionManager.getInstance().regionsAt(player.getLocation()));
    }

    private void updateTabRegionName(RegionEvent event) {
        updateTabRegionName(event.getPlayer(), event.getRegions());
    }

    private void updateTabRegionName(Player player, ApplicableRegionSet regions) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player);
        if (smpPlayer == null) return;

        String name = regions.queryValue(null, SmpRegionFlags.DISPLAY_NAME);
        smpPlayer.getTabContext().set(TabKeys.REGION_NAME, name == null || name.isBlank() ? null : name);
    }
}
