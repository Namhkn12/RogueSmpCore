package com.roguesmp.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArrowConsumeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final ItemStack mArrow;

    private boolean mCancelled = false;

    public ArrowConsumeEvent(Player player, ItemStack arrow) {
        super(player);
        this.mArrow = arrow;
    }

    @Override
    public boolean isCancelled() {
        return mCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        mCancelled = cancel;
    }

    public ItemStack getArrow() {
        return mArrow;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
