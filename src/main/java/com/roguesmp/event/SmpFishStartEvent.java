package com.roguesmp.event;

import com.roguesmp.fishing.FishTier;
import com.roguesmp.fishing.FishingMinigameSettings;
import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Holder;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Called when a fishing minigame start
 */
public class SmpFishStartEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final FishHook hook;
    private final @Nullable String hotspotId;
    private final List<Holder<LootTable>> lootTables = new ArrayList<>();
    private FishTier tier;
    private FishingMinigameSettings settings;
    private double luckBonus;
    private boolean cancelled;

    public SmpFishStartEvent(Player player, FishHook hook, @Nullable String hotspotId, FishTier tier) {
        super(player);
        this.hook = hook;
        this.hotspotId = hotspotId;
        this.tier = tier;
        this.settings = FishingMinigameSettings.of(tier);
        this.lootTables.add(tier.lootTable());
    }

    public FishHook getHook() {
        return hook;
    }

    public @Nullable String getHotspotId() {
        return hotspotId;
    }

    public FishTier getTier() {
        return tier;
    }

    /**
     * Resets the settings to the new tier's defaults and swaps the old tier's loot table for the new one.
     */
    public void setTier(FishTier tier) {
        int index = lootTables.indexOf(this.tier.lootTable());
        if (index >= 0) lootTables.set(index, tier.lootTable());
        this.tier = tier;
        this.settings = FishingMinigameSettings.of(tier);
    }

    public FishingMinigameSettings getSettings() {
        return settings;
    }

    public void setSettings(FishingMinigameSettings settings) {
        this.settings = settings;
    }

    public List<Holder<LootTable>> getLootTables() {
        return lootTables;
    }

    public double getLuckBonus() {
        return luckBonus;
    }

    public void setLuckBonus(double luckBonus) {
        this.luckBonus = luckBonus;
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
