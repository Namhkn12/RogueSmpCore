package com.roguesmp.dungeon.listener;

import com.roguesmp.constant.Keys;
import com.roguesmp.utils.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

public class RewardChestListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String table = event.getItemInHand().getPersistentDataContainer().get(Keys.REWARD_CID, PersistentDataType.STRING);
        if (table == null || !(event.getBlockPlaced().getState() instanceof TileState tile)) return;

        tile.getPersistentDataContainer().set(Keys.REWARD_CID, PersistentDataType.STRING, table);
        tile.update();
        event.getPlayer().sendMessage(Utils.fromString("<gray>Đã đặt rương thưởng <white>" + table + "<gray>."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(RewardChestListener::isRewardChest);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(RewardChestListener::isRewardChest);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(RewardChestListener::isRewardChest)) event.setCancelled(true);
    }

    private static boolean isRewardChest(Block block) {
        return block.getState() instanceof TileState tile && tile.getPersistentDataContainer().has(Keys.REWARD_CID, PersistentDataType.STRING);
    }
}
