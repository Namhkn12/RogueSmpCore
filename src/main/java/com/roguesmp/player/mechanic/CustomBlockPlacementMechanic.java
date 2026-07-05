package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.BlockRegistry;
import org.bukkit.event.block.BlockPlaceEvent;

public class CustomBlockPlacementMechanic implements PlayerMechanic {
    @Override public int getPriority() { return 15; }

    @Override
    public void onBlockPlace(BlockPlaceEvent event, SmpPlayer player) {
        EquipSlot equipSlot = EquipSlot.fromVanilla(event.getHand());
        SmpItem smpItem = player.getItemAtEquipSlot(equipSlot);
        if (smpItem != null && smpItem.getBaseItem() != null && BlockRegistry.getBlock(smpItem.getBaseItem().getId()) == null) {
            event.setCancelled(true); // Prevent placing custom structural items
        }
    }
}
