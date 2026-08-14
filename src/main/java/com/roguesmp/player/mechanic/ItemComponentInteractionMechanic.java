package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Fans {@link PlayerInteractEvent}/{@link PlayerInteractEntityEvent} out to every
 * {@link InteractableComponent} on the interacting hand's {@code SmpItem} - same
 * instanceof-mixin shape {@link com.roguesmp.item.component.UniqueTrackingComponent} established,
 * as an actual per-event callback instead of a presence check.
 */
public class ItemComponentInteractionMechanic implements PlayerMechanic {

    @Override
    public int getPriority() {
        return 505;
    }

    @Override
    public void onInteract(PlayerInteractEvent event, SmpPlayer player) {
        SmpItem smpItem = player.getItemAtEquipSlot(EquipSlot.fromVanilla(event.getHand()));
        if (smpItem == null) return;

        for (ItemComponent component : smpItem.getComponents().values()) {
            if (component instanceof InteractableComponent interactable) {
                interactable.onItemInteract(player, event);
            }
        }
    }

    @Override
    public void onEntityInteract(PlayerInteractEntityEvent event, SmpPlayer player) {
        SmpItem smpItem = player.getItemAtEquipSlot(EquipSlot.fromVanilla(event.getHand()));
        if (smpItem == null) return;

        for (ItemComponent component : smpItem.getComponents().values()) {
            if (component instanceof InteractableComponent interactable) {
                interactable.onEntityInteract(player, event);
            }
        }
    }
}
