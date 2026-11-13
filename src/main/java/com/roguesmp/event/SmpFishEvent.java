package com.roguesmp.event;

import com.roguesmp.fishing.FishTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Called when player successfully completed the minigame and caught the fish.
 */
public class SmpFishEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final FishTier tier;
    private final @Nullable String hotspotId;
    private final List<ItemStack> caught;
    private boolean cancelled;

    public SmpFishEvent(Player player, FishTier tier, @Nullable String hotspotId, List<ItemStack> caught) {
        super(player);
        this.tier = tier;
        this.hotspotId = hotspotId;
        this.caught = caught;
    }

    public FishTier getTier() {
        return tier;
    }

    public @Nullable String getHotspotId() {
        return hotspotId;
    }

    /**
     * Mutable; listeners may add, remove or replace the items that will drop.
     */
    public List<ItemStack> getCaught() {
        return caught;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
