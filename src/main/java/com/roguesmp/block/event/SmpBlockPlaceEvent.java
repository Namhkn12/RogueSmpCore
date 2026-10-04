package com.roguesmp.block.event;

import com.roguesmp.block.SmpBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by {@code BlockManager#place} once the vanilla {@code BlockPlaceEvent} has already passed, right before the display entity is spawned and the block is
 * registered. Cancelling this restores whatever was at {@link #getTarget()} beforehand, same as a
 * cancelled vanilla placement.
 */
public class SmpBlockPlaceEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull SmpBlock block;
    private final @NotNull Block target;
    private boolean cancelled;

    public SmpBlockPlaceEvent(@NotNull Player player, @NotNull SmpBlock block, @NotNull Block target) {
        this.player = player;
        this.block = block;
        this.target = target;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /** The block about to be placed - created, but not yet bound to a position or registered. */
    public @NotNull SmpBlock getBlock() {
        return block;
    }

    /** The vanilla block being replaced - already a barrier by the time this fires. */
    public @NotNull Block getTarget() {
        return target;
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
