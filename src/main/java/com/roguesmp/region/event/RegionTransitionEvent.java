package com.roguesmp.region.event;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class RegionTransitionEvent extends PlayerEvent {

    public record Occupancy(World world, Set<String> regionIds) {

        public static Occupancy empty(World world) {
            return new Occupancy(world, Set.of());
        }

        public boolean contains(String regionId) {
            return regionIds.contains(regionId);
        }
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Occupancy from;
    private final Occupancy to;

    public RegionTransitionEvent(Player player, Occupancy from, Occupancy to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public Occupancy getFrom() {
        return from;
    }

    public Occupancy getTo() {
        return to;
    }

    public Set<String> getEnteredRegions() {
        Set<String> entered = new HashSet<>(to.regionIds());
        if (from.world().equals(to.world())) entered.removeAll(from.regionIds());
        return entered;
    }

    public Set<String> getLeftRegions() {
        Set<String> left = new HashSet<>(from.regionIds());
        if (from.world().equals(to.world())) left.removeAll(to.regionIds());
        return left;
    }

    public boolean hasEntered(String regionId) {
        return getEnteredRegions().contains(regionId);
    }

    public boolean hasLeft(String regionId) {
        return getLeftRegions().contains(regionId);
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
