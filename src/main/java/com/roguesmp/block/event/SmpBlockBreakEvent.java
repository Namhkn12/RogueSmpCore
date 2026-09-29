package com.roguesmp.block.event;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired by {@code BlockManager#destroy}, right before any teardown (drops, state removal, display
 * removal) runs. Cancelling this also cancels the underlying {@link #getBreakEvent()}.
 */
public class SmpBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull SmpBlock block;
    private final @NotNull SmpPlayer player;
    private final @NotNull BlockBreakEvent breakEvent;
    private final @NotNull List<ItemStack> drops;
    private int experience;
    private boolean cancelled;

    public SmpBlockBreakEvent(@NotNull SmpBlock block, @NotNull SmpPlayer player, @NotNull BlockBreakEvent breakEvent,
                               @NotNull List<ItemStack> drops, int experience) {
        this.block = block;
        this.player = player;
        this.breakEvent = breakEvent;
        this.drops = drops;
        this.experience = experience;
    }

    public @NotNull SmpBlock getBlock() {
        return block;
    }

    public @NotNull SmpPlayer getPlayer() {
        return player;
    }

    public @NotNull BlockBreakEvent getBreakEvent() {
        return breakEvent;
    }

    /** Return a mutable list that this event will drops */
    public @NotNull List<ItemStack> getDrops() {
        return drops;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
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
