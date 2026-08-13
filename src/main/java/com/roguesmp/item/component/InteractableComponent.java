package com.roguesmp.item.component;

import com.roguesmp.player.SmpPlayer;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Mixin for {@link ItemComponent}s that want to react when a player interacts (right/left-click)
 * while holding the item, or right-clicks an entity while holding it. Dispatched by
 * {@code player.mechanic.ItemComponentInteractionMechanic}, which iterates the interacting hand's
 * {@code SmpItem} components and calls whichever hook applies to any that implement this - same
 * instanceof-mixin shape as {@link UniqueTrackingComponent}.
 */
public interface InteractableComponent extends ItemComponent {
    default void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {

    }

    default void onEntityInteract(SmpPlayer smpPlayer, PlayerInteractEntityEvent event) {

    }
}
