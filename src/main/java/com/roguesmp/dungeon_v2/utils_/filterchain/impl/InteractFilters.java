package com.roguesmp.dungeon_v2.utils_.filterchain.impl;

import com.roguesmp.dungeon_v2.utils_.filterchain.EventFilter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class InteractFilters {

    public static EventFilter<PlayerInteractEvent> action(Action action) {
        return event -> event.getAction() == action;
    }

    public static EventFilter<PlayerInteractEvent> rightClickBlock() {
        return action(Action.RIGHT_CLICK_BLOCK);
    }

    public static EventFilter<PlayerInteractEvent> leftClickBlock() {
        return action(Action.LEFT_CLICK_BLOCK);
    }

    public static EventFilter<PlayerInteractEvent> mainHand() {
        return event -> event.getHand() == EquipmentSlot.HAND;
    }

    public static EventFilter<PlayerInteractEvent> hasBlock() {
        return event -> event.getClickedBlock() != null;
    }

    public static EventFilter<PlayerInteractEvent> blockType(Material material) {
        return event -> {
            Block block = event.getClickedBlock();
            return block != null && block.getType() == material;
        };
    }

    public static EventFilter<PlayerInteractEvent> blockState(Class<?> stateClass) {
        return event -> {
            Block block = event.getClickedBlock();
            return block != null && stateClass.isInstance(block.getState());
        };
    }

    public static EventFilter<PlayerInteractEvent> clickedBlockInWorld(String prefix) {
        return event -> {
            Block block = event.getClickedBlock();
            return block != null && block.getWorld().getName().startsWith(prefix);
        };
    }

    public static EventFilter<PlayerInteractEvent> itemInHand(Material material) {
        return event -> event.getItem() != null && event.getItem().getType() == material;
    }
}