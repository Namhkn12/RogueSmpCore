package com.roguesmp.region.listener;

import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.flag.EnterTeleport;
import com.roguesmp.teleport.TeleportService;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RegionTeleportListener implements Listener {

    @EventHandler
    public void onRegionEnter(RegionEnterEvent event) {
        ProtectedRegion settings = ServerRegionManager.getInstance().get(event.getWorld(), event.getRegionId());
        EnterTeleport teleport = settings == null ? null : settings.getFlag(SmpRegionFlags.ENTER_TELEPORT);
        if (teleport == null) return;

        TeleportService.getInstance().teleport(event.getPlayer(), teleport.destination(), teleport.options());
    }
}
