package com.roguesmp.region.event;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public abstract class RegionEvent extends PlayerEvent {

    private final World world;
    private final String regionId;
    private final ApplicableRegionSet regions;

    protected RegionEvent(Player player, World world, String regionId, ApplicableRegionSet regions) {
        super(player);
        this.world = world;
        this.regionId = regionId;
        this.regions = regions;
    }

    public World getWorld() {
        return world;
    }

    public String getRegionId() {
        return regionId;
    }

    public ApplicableRegionSet getRegions() {
        return regions;
    }
}
