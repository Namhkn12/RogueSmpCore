package com.roguesmp.fishing.listener;

import com.roguesmp.fishing.FishingHotspotManager;
import com.roguesmp.fishing.FishingHotspotMessages;
import com.roguesmp.fishing.FishingManager;
import com.roguesmp.region.event.RegionEnterEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FishingListener implements Listener {

    private final FishingManager fishingManager;
    private final FishingHotspotManager hotspotManager;

    public FishingListener(FishingManager fishingManager, FishingHotspotManager hotspotManager) {
        this.fishingManager = fishingManager;
        this.hotspotManager = hotspotManager;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case BITE -> fishingManager.start(event.getPlayer(), event.getHook());
            case CAUGHT_FISH -> {
                if (!fishingManager.isFishing(event.getPlayer())) return;
                event.setCancelled(true);
                fishingManager.abort(event.getPlayer());
            }
            default -> { }
        }
    }

    @EventHandler
    public void onRegionEnter(RegionEnterEvent event) {
        FishingHotspotManager.ActiveHotspot active = hotspotManager.getActive(event.getWorld(), event.getRegionId());
        if (active != null) FishingHotspotMessages.sendEntered(event.getPlayer(), active);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fishingManager.abort(event.getPlayer());
    }
}
