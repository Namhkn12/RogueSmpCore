package com.roguesmp.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when player start blocking with an item (shield, item with Blocking component).
 * <p>
 *     If the event is canceled, it will also mark the related PlayerInteractEvent as canceled. See {@link com.roguesmp.listener.PlayerListener#onInteract(PlayerInteractEvent)}
 * </p>
 */
public class PlayerStartBlockAttackEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack item;
    private final Player player;
    private final EquipmentSlot hand;

    private boolean cancelled;

    public PlayerStartBlockAttackEvent(ItemStack item, Player player, EquipmentSlot hand) {
        this.item = item;
        this.player = player;
        this.hand = hand;
    }

    public EquipmentSlot getHand() {
        return hand;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
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

    /**
     * Cancel this event, this will also mark the related PlayerInteractEvent to be canceled. See {@link com.roguesmp.listener.PlayerListener#onInteract(PlayerInteractEvent)}
     * @param cancel {@code true} if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
