package com.roguesmp.region.event;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RegionEnterEvent extends RegionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public RegionEnterEvent(Player player, World world, String regionId, ApplicableRegionSet regions) {
        super(player, world, regionId, regions);
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
