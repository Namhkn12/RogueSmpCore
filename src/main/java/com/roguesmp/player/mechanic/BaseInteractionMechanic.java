package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.gui.crafting.CraftingGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.BlockRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BaseInteractionMechanic implements PlayerMechanic {
    @Override public int getPriority() { return 15; }

    @Override
    public void onBlockPlace(BlockPlaceEvent event, SmpPlayer player) {
        EquipSlot equipSlot = EquipSlot.fromVanilla(event.getHand());
        SmpItem smpItem = player.getItemAtEquipSlot(equipSlot);
        if (smpItem != null && smpItem.getBaseItem() != null && BlockRegistry.getBlock(smpItem.getBaseItem().getId()) == null) {
            event.setCancelled(true); // Prevent placing custom structural items
        }
    }

    @Override
    public void onInteract(PlayerInteractEvent event, SmpPlayer player) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block.getType() == Material.CRAFTING_TABLE) {
            event.setCancelled(true);
            new CraftingGui(event.getPlayer()).showInventory(event.getPlayer());
        }
    }
}
