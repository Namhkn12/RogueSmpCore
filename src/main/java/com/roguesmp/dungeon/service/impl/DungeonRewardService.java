package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.definition.loot.LootContext;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.dto.loot.LootRules;
import com.roguesmp.dungeon.itemdisplay.impl.ChestOpenAnimation;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DungeonRewardService implements IDungeonRewardService {

    private final ILootService lootService;
    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final DungeonManager dungeonManager;
    private final ChestOpenAnimation chestOpenAnimation;

    public DungeonRewardService(ILootService lootService, IPartyService partyService, InstanceManager instanceManager, DungeonManager dungeonManager, ChestOpenAnimation chestOpenAnimation) {
        this.lootService = lootService;
        this.partyService = partyService;
        this.instanceManager = instanceManager;
        this.dungeonManager = dungeonManager;
        this.chestOpenAnimation = chestOpenAnimation;
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
        if (!e.hasBlock()) return false;
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return false;
        if (!block.getWorld().getName().startsWith("dungeon_")) return false;
        if (!(block.getState() instanceof TileState tileState)) return false;

        PersistentDataContainer blockPdc = tileState.getPersistentDataContainer();
        String cidValue = blockPdc.get(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING);
        if (cidValue == null || cidValue.isBlank()) return false;

        e.setCancelled(true);
        tileState.getPersistentDataContainer().remove(NameSpaceKeys.REWARD_CID_KEY);
        tileState.update();

        Player player = e.getPlayer();

        String tableId;
        LootContext ctx;

        if ("dungeon".equals(cidValue)) {
            Party party = partyService.getPartyByPlayer(player);
            if (party == null) return false;
            String instanceId = party.getInstanceId();
            if (instanceId == null || instanceId.isBlank()) return false;
            DungeonInstance instance = instanceManager.get(instanceId);
            if (instance == null) return false;
            Dungeon dungeon = dungeonManager.get(instance.getSession().getDungeonId());
            if (dungeon == null) return false;

            tableId = dungeon.getLootTableId();
            int totalScore = instance.getProgress().getScore();
            ctx = LootContext.builder()
                    .addRule(new LootRules.DungeonScoreRule(totalScore, 0.01))
                    .build();
        } else {
            tableId = cidValue;
            ctx = LootContext.builder().build();
        }

        if (!lootService.exists(tableId)) {
            player.sendMessage("§c[Chest] Loot table not found: §f" + tableId);
            return true;
        }

        List<ItemStack> items = lootService.roll(tableId, ctx);

        if (isDoubleSide(block)) {
            items.addAll(lootService.roll(tableId, ctx));
        }

        Chest chest = (Chest) block.getState();
        Inventory inv = chest.getBlockInventory();
        scatterItems(inv, items);

        chestOpenAnimation.play(player, block, items);

        return true;
    }


    private boolean isDoubleSide(Block block) {
        if (!(block.getState() instanceof Chest chest)) return false;
        if (!(chest.getInventory() instanceof DoubleChestInventory)) return false;
        Block other = getOtherBlock(block);
        if (other == null || !(other.getState() instanceof TileState ts)) return false;
        return "dungeon".equals(ts.getPersistentDataContainer()
                .get(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING));
    }

    private Block getOtherBlock(Block block) {
        if (!(block.getState() instanceof Chest chest)) return null;
        if (!(chest.getInventory() instanceof DoubleChestInventory dci)) return null;
        DoubleChest dc = (DoubleChest) dci.getHolder();
        if (dc == null) return null;
        Chest left  = (Chest) dc.getLeftSide();
        Chest right = (Chest) dc.getRightSide();
        if (left  != null && !left.getBlock().equals(block))  return left.getBlock();
        if (right != null && !right.getBlock().equals(block)) return right.getBlock();
        return null;
    }

    private void scatterItems(Inventory inv, List<ItemStack> items) {
        inv.clear();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots);
        for (int i = 0; i < Math.min(items.size(), slots.size()); i++) {
            inv.setItem(slots.get(i), items.get(i));
        }
    }
}
