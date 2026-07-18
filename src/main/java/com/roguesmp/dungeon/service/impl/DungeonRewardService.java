package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.loot.rule.LootRules;
import com.roguesmp.dungeon.itemdisplay.impl.ChestOpenAnimation;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.loot.service.ILootService;
import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import com.roguesmp.utils.PlayerUtils;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class DungeonRewardService implements IDungeonRewardService {

    /** Modifier bonus-roll cộng thêm cho rương đôi → cơ hội (số lần roll thưởng) tốt hơn. */
    private static final double DOUBLE_CHEST_LUCK_BONUS = 1.0;

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
            DungeonEcho.warn(e.getPlayer(), "Không thể phá rương thưởng");
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

        Player player = e.getPlayer();

        if (hasClaimed(block, player)) {
            chestOpenAnimation.playAlreadyClaimed(player, block);
            return true;
        }

        boolean isDouble = getOtherBlock(block) != null;

        String tableId;
        LootContext.Builder ctxBuilder = LootContext.builder();

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
            ctxBuilder.addRule(new LootRules.DungeonScoreRule(totalScore, 0.01));
        } else {
            tableId = cidValue;
        }

        if (isDouble) {
            ctxBuilder.addRule(new LootRules.Fixed(DOUBLE_CHEST_LUCK_BONUS));
        }

        LootContext ctx = ctxBuilder.build();

        if (!lootService.exists(tableId)) {
            player.sendMessage("§c[Chest] Loot table not found: §f" + tableId);
            return true;
        }

        List<ItemStack> items = lootService.roll(tableId, ctx);

        if (isDouble) {
            items.addAll(lootService.roll(tableId, ctx));
        }

        markClaimed(block, player);

        chestOpenAnimation.play(player, block, items, () -> PlayerUtils.giveItem(player, items));

        return true;
    }

    private boolean hasClaimed(Block block, Player player) {
        String id = player.getUniqueId().toString();
        if (containsClaim(block, id)) return true;
        Block other = getOtherBlock(block);
        return other != null && containsClaim(other, id);
    }

    private boolean containsClaim(Block block, String id) {
        if (!(block.getState() instanceof TileState ts)) return false;
        List<String> list = ts.getPersistentDataContainer()
                .get(NameSpaceKeys.REWARD_CLAIMED_KEY, PersistentDataType.LIST.strings());
        return list != null && list.contains(id);
    }

    private void markClaimed(Block block, Player player) {
        String id = player.getUniqueId().toString();
        addClaim(block, id);
        Block other = getOtherBlock(block);
        if (other != null) addClaim(other, id);
    }

    private void addClaim(Block block, String id) {
        if (!(block.getState() instanceof TileState ts)) return;
        PersistentDataContainer pdc = ts.getPersistentDataContainer();
        List<String> current = pdc.get(NameSpaceKeys.REWARD_CLAIMED_KEY, PersistentDataType.LIST.strings());
        List<String> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (!updated.contains(id)) updated.add(id);
        pdc.set(NameSpaceKeys.REWARD_CLAIMED_KEY, PersistentDataType.LIST.strings(), updated);
        ts.update();
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
}
