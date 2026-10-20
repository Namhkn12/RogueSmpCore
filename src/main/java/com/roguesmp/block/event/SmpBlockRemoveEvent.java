package com.roguesmp.block.event;

import com.roguesmp.block.SmpBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a block is permanently removed from the world, whether broken by a player or not. Not fired when its chunk or world unloads.
 */
public class SmpBlockRemoveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull SmpBlock block;

    public SmpBlockRemoveEvent(@NotNull SmpBlock block) {
        this.block = block;
    }

    public @NotNull SmpBlock getBlock() {
        return block;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
