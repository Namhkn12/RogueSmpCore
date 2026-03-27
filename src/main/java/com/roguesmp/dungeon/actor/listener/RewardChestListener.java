package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.actor.command.RewardCommand;
import com.roguesmp.dungeon.service.impl.RewardService;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class RewardChestListener implements Listener {

    private final RewardService rewardService;

    public RewardChestListener(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;

        PersistentDataContainer itemPdc = item.getItemMeta().getPersistentDataContainer();
        if (!itemPdc.has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE)) return;

        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof TileState tileState)) return;

        Player player = event.getPlayer();

        // Copy PDC tag từ item sang block, owner mặc định là người đặt
        rewardService.tagRewardChest(block, player.getUniqueId());

        player.sendMessage("§aReward Chest đã được đặt!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Chỉ xử lý click chuột phải vào block, bỏ qua tay trái (off-hand)
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        if (!pdc.has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE)) return;

        // Block là reward chest → cancel mở inventory
        event.setCancelled(true);

        Location loc = block.getLocation();
        Player player = event.getPlayer();

        // Kiểm tra chest có đang trong animation không (tránh spam click)
        if (activeChests.contains(loc)) return;

        // Kiểm tra player có phải owner của chest không
        String ownerStr = pdc.get(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING);
        if (ownerStr == null) return;
        UUID ownerId = UUID.fromString(ownerStr);
        if (!player.getUniqueId().equals(ownerId)) {
            player.sendMessage(ChatColor.RED + "Đây không phải rương của bạn!");
            return;
        }

        // Bắt đầu sequence: animation → bắn item
        activeChests.add(loc);
        openRewardChest(player, block, ownerId);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (!(block.getState() instanceof TileState ts)) return false;
            return ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (!(block.getState() instanceof TileState ts)) return false;
            return ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (!(block.getState() instanceof TileState ts)) continue;
            if (ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}