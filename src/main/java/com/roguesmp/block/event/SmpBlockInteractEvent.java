package com.roguesmp.block.event;

import com.roguesmp.block.SmpBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a player's click is handed to {@link SmpBlock#onBlockInteract}. Cancelling it stops the block from handling the click and denies the underlying interaction.
 */
public class SmpBlockInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull SmpBlock block;
    private final @NotNull PlayerInteractEvent interactEvent;
    private boolean cancelled;

    public SmpBlockInteractEvent(@NotNull Player player, @NotNull SmpBlock block, @NotNull PlayerInteractEvent interactEvent) {
        this.player = player;
        this.block = block;
        this.interactEvent = interactEvent;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull SmpBlock getBlock() {
        return block;
    }

    public @NotNull PlayerInteractEvent getInteractEvent() {
        return interactEvent;
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
