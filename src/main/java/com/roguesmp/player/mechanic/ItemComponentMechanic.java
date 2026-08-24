package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.TickingComponent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ItemComponentMechanic implements PlayerMechanic {

    @Override
    public int getPriority() {
        return 505;
    }

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        player.getCurrentEquipment().forEach((equipSlot, smpItem) -> {
            for (ItemComponent component : smpItem.getComponents().values()) {
                if (component instanceof TickingComponent ticking) {
                    ticking.tick(player, smpItem, equipSlot, periodIncrement);
                }
            }
        });
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
