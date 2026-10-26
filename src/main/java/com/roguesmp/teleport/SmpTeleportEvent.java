package com.roguesmp.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmpTeleportEvent extends PlayerEvent implements Cancellable {

    public enum Phase { WARMUP, DEPART }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Phase phase;
    private final TeleportDestination destination;
    private final TeleportOptions options;
    private final @Nullable Location target;
    private boolean cancelled;

    public SmpTeleportEvent(Player player, Phase phase, TeleportDestination destination, TeleportOptions options, @Nullable Location target) {
        super(player);
        this.phase = phase;
        this.destination = destination;
        this.options = options;
        this.target = target;
    }

    public Phase getPhase() {
        return phase;
    }

    public TeleportDestination getDestination() {
        return destination;
    }

    public TeleportOptions getOptions() {
        return options;
    }

    public @Nullable Location getTarget() {
        return target == null ? null : target.clone();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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
