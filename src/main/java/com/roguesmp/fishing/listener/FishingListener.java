package com.roguesmp.fishing.listener;

import com.roguesmp.event.SmpFishStartEvent;
import com.roguesmp.fishing.FishingHotspot;
import com.roguesmp.fishing.FishingHotspotManager;
import com.roguesmp.fishing.FishingHotspotMessages;
import com.roguesmp.fishing.FishingManager;
import com.roguesmp.fishing.FishingMinigameSettings;
import com.roguesmp.region.event.RegionEnterEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
            case BITE -> {
                if (!fishingManager.start(event.getPlayer(), event.getHook())) event.setCancelled(true);
            }
            case REEL_IN -> fishingManager.escape(event.getPlayer());
            case CAUGHT_FISH -> {
                if (!fishingManager.isFishing(event.getPlayer())) return;
                event.setCancelled(true);
                fishingManager.escape(event.getPlayer());
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFishStart(SmpFishStartEvent event) {
        FishingHotspotManager.ActiveHotspot active = hotspotManager.findAt(event.getHook().getLocation());
        if (active == null) return;
        FishingHotspot hotspot = active.hotspot();

        FishingMinigameSettings settings = event.getSettings();
        event.setSettings(settings
                .withBarSegments(settings.barSegments() + hotspot.barSegmentsBonus())
                .withBarTurnChance(settings.barTurnChance() * hotspot.barTurnChanceMultiplier()));
        event.setLuckBonus(event.getLuckBonus() + hotspot.luckBonus());

        if (hotspot.lootTables().isEmpty()) return;
        if (hotspot.lootMode() == FishingHotspot.LootMode.REPLACE) event.getLootTables().clear();
        event.getLootTables().addAll(hotspot.lootTables());
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
