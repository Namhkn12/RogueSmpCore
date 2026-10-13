package com.roguesmp.dungeon.service;

import com.roguesmp.constant.Keys;
import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.dungeon_party.PartyManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.dungeon.presentation.presenter.ChestOpenAnimation;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Registries;
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
import java.util.stream.Stream;

public class DungeonRewardService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;
    private final DungeonManager dungeonManager;
    private final ChestOpenAnimation chestOpenAnimation;

    public DungeonRewardService(PartyManager partyManager, InstanceManager instanceManager, DungeonManager dungeonManager, ChestOpenAnimation chestOpenAnimation) {
        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
        this.dungeonManager = dungeonManager;
        this.chestOpenAnimation = chestOpenAnimation;
    }

    public boolean onPlace(BlockPlaceEvent e) {
        ItemStack inHand = e.getItemInHand();
        if(inHand.getType() != Material.CHEST) return false;
        PersistentDataContainer itemPdc = inHand.getItemMeta().getPersistentDataContainer();
        if(!itemPdc.has(Keys.REWARD_CID, PersistentDataType.STRING)) return false;
        String rewardCid = itemPdc.get(Keys.REWARD_CID, PersistentDataType.STRING);
        // inject metadata into chest block
        Block chest = e.getBlockPlaced();
        if(!(chest.getState() instanceof TileState tileState)) return false;
        PersistentDataContainer lootChest = tileState.getPersistentDataContainer();
        lootChest.set(Keys.REWARD_CID, PersistentDataType.STRING, rewardCid);
        tileState.update();
        e.getPlayer().sendMessage(Component.text("Place a reward chest", NamedTextColor.GRAY));
        return true;
    }

    public boolean onBreak(BlockBreakEvent e) {
        if(!DungeonWorldManager.isAnyDungeonWorld(e.getBlock().getWorld())) return false;
        Block block = e.getBlock();
        if(block.getType() != Material.CHEST) return false;

        if(!(block.getState() instanceof TileState tileState)) return false;

        PersistentDataContainer blockPdc = tileState.getPersistentDataContainer();
        if(blockPdc.has(Keys.REWARD_CID, PersistentDataType.STRING)){
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("Không thể phá rương thưởng", NamedTextColor.YELLOW));
            return true;
        }

        return false;
    }

    public boolean onOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if (!e.hasBlock()) return false;
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return false;
        if (!DungeonWorldManager.isAnyDungeonWorld(block.getWorld())) return false;
        if (!(block.getState() instanceof TileState tileState)) return false;

        PersistentDataContainer blockPdc = tileState.getPersistentDataContainer();
        String cidValue = blockPdc.get(Keys.REWARD_CID, PersistentDataType.STRING);
        if (cidValue == null || cidValue.isBlank()) return false;

        e.setCancelled(true);

        Player player = e.getPlayer();

        if (hasClaimed(block, player)) {
            chestOpenAnimation.playAlreadyClaimed(player, block);
            return true;
        }

        boolean isDouble = getOtherBlock(block) != null;

        String tableId;

        if ("dungeon".equals(cidValue)) {
            Party party = partyManager.getPartyByPlayer(player);
            if (party == null) return false;
            String instanceId = party.getInstanceId();
            if (instanceId == null || instanceId.isBlank()) return false;
            DungeonInstance instance = instanceManager.get(instanceId);
            if (instance == null) return false;
            Dungeon dungeon = dungeonManager.get(instance.getSession().getDungeonId());
            if (dungeon == null) return false;

            tableId = dungeon.getLootTableId();
        } else {
            tableId = cidValue;
        }

        LootContext ctx = LootContext.builder(PlayerManager.getInstance().getSmpPlayer(player))
                .origin(LootOrigin.CHEST, block)
                .build();

        LootTable table = Registries.LOOT_TABLE.get(tableId);
        if (table == null) {
            player.sendMessage("§c[Chest] Loot table not found: §f" + tableId);
            return true;
        }

        List<ItemStack> rolled = table.roll(ctx);
        List<ItemStack> items = isDouble
                ? Stream.concat(rolled.stream(), table.roll(ctx).stream()).toList()
                : rolled;

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
                .get(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings());
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
        List<String> current = pdc.get(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings());
        List<String> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (!updated.contains(id)) updated.add(id);
        pdc.set(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings(), updated);
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
