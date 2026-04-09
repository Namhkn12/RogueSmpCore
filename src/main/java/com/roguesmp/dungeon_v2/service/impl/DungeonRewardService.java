package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.loot.LootContext;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.dto.loot.LootRules;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class DungeonRewardService implements IDungeonRewardService {

    private final ILootService lootService;
    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final DungeonManager dungeonManager;

    public DungeonRewardService(ILootService lootService, IPartyService partyService, InstanceManager instanceManager, DungeonManager dungeonManager) {
        this.lootService = lootService;
        this.partyService = partyService;
        this.instanceManager = instanceManager;
        this.dungeonManager = dungeonManager;
    }

    @Override
    public boolean onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if(inHand.getType() != Material.CHEST) return false;
        PersistentDataContainer itemPdc = inHand.getItemMeta().getPersistentDataContainer();
        if(!itemPdc.has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING)) return false;
        String rewardCid = itemPdc.get(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING);
        // inject metadata into chest block
        Block chest = e.getBlockPlaced();
        if(!(chest.getState() instanceof TileState tileState)) return false;
        PersistentDataContainer lootChest = tileState.getPersistentDataContainer();
        lootChest.set(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING, rewardCid);
        tileState.update();
        DungeonEcho.info(e.getPlayer(), "Place a reward chest");
        return true;
    }

    @Override
    public boolean onBreak(BlockBreakEvent e) {
        if(!e.getBlock().getWorld().getName().startsWith("dungeon_")) return false;
        Block block = e.getBlock();
        if(block.getType() != Material.CHEST) return false;

        if(!(block.getState() instanceof TileState tileState)) return false;

        PersistentDataContainer blockPdc = tileState.getPersistentDataContainer();
        if(blockPdc.has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING)){
            e.setCancelled(true);
            DungeonEcho.warn(e.getPlayer(), "Không thể phá rương khi chưa nhận thưởng");
            return true;
        }

        return false;
    }

    @Override
    public boolean onOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if(!e.hasBlock()) return false;
        Block block = e.getClickedBlock();
        if(block == null || block.getType() !=  Material.CHEST) return false;
        if(!block.getWorld().getName().startsWith("dungeon_")) return false;
        if(!(block.getState() instanceof TileState tileState)) return false;
        PersistentDataContainer blockPdc = tileState.getPersistentDataContainer();
        if(blockPdc.has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING)){
            e.setCancelled(true);

            Player player = e.getPlayer();
            Party party = partyService.getPartyByPlayer(player);
            if(party == null) return false;
            DungeonInstance instance = instanceManager.get(party.getInstanceId());
            if(instance == null) return false;
            Dungeon dungeon = dungeonManager.get(instance.getSession().getDungeonId());
            if(dungeon == null) return false;

            int totalScore = instance.getProgress().getScore();
            LootContext ctx = LootContext.builder()
                    .addRule(new LootRules.DungeonScoreRule(totalScore, 0.01)) // 0.01 -> max score = 100
                    // có thể stack thêm rule khác
                    // .addRule(new LootRules.DungeonTier(instance.getTier(), 0.25))
                    .build();
            Chest chest = (Chest) block.getState();
            Inventory chestInventory = chest.getInventory();

            blockPdc.remove(NameSpaceKeys.REWARD_CID_KEY);
            tileState.update();

            List<ItemStack> items = lootService.roll(dungeon.getLootTableId(), ctx);
            chestInventory.clear();
            for (ItemStack item : items) {
                chestInventory.addItem(item);
            }

            player.openInventory(chestInventory);
            return true;
        }
        return false;
    }
}
