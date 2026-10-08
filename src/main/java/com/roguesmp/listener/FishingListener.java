package com.roguesmp.listener;

import com.roguesmp.fishing.FishingManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FishingListener implements Listener {

    private final FishingManager fishingManager;

    public FishingListener(FishingManager fishingManager) {
        this.fishingManager = fishingManager;
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
    public void onQuit(PlayerQuitEvent event) {
        fishingManager.abort(event.getPlayer());
    }
}
