package com.roguesmp.block.event;

import com.roguesmp.block.SmpBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code BlockManager#destroy}, right before any teardown (drops, state removal, display
 * removal) runs. Cancelling this also cancels the underlying {@link #getBreakEvent()}.
 */
public class SmpBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull SmpBlock block;
    private final @NotNull Player player;
    private final @NotNull BlockBreakEvent breakEvent;
    private boolean cancelled;

    public SmpBlockBreakEvent(@NotNull SmpBlock block, @NotNull Player player, @NotNull BlockBreakEvent breakEvent) {
        this.block = block;
        this.player = player;
        this.breakEvent = breakEvent;
    }

    public @NotNull SmpBlock getBlock() {
        return block;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /** The vanilla event this fired from - inspect it for drop/exp settings, but don't cancel it directly, cancel this instead. */
    public @NotNull BlockBreakEvent getBreakEvent() {
        return breakEvent;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
