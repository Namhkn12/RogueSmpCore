package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.DungeonSignal;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.dungeon_core.NoState;
import com.roguesmp.dungeon_core.presentation.ChestOpenAnimation;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record RewardChestsFeature(String lootTable) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "reward_chests";
    public static final String DUNGEON_TABLE = "dungeon";

    private static final ChestOpenAnimation ANIMATION = new ChestOpenAnimation();

    public static final Codec<RewardChestsFeature> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("loot_table", "").forGetter(RewardChestsFeature::lootTable),
            RewardChestsFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }

    @Override
    public void onSignal(FeatureContext<NoState> context, DungeonSignal signal) {
        switch (signal) {
            case DungeonSignal.BlockInteracted interacted when tableOf(interacted.block()) != null -> {
                interacted.event().setCancelled(true);
                open(interacted.player(), interacted.block());
            }
            case DungeonSignal.BlockBroken broken when tableOf(broken.block()) != null -> {
                broken.event().setCancelled(true);
                broken.player().sendMessage(Utils.fromString("<yellow>Không thể phá rương thưởng."));
            }
            default -> {
            }
        }
    }

    private void open(Player player, Block block) {
        if (hasClaimed(block, player)) {
            ANIMATION.playAlreadyClaimed(player, block);
            return;
        }

        String marked = tableOf(block);
        String tableId = DUNGEON_TABLE.equals(marked) ? lootTable : marked;
        LootTable table = tableId == null ? null : Registries.LOOT_TABLE.get(tableId);
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (table == null || smpPlayer == null) {
            player.sendMessage(Utils.fromString("<red>Rương này chưa có phần thưởng (" + tableId + ")."));
            return;
        }

        LootContext lootContext = LootContext.builder(smpPlayer).origin(LootOrigin.CHEST, block).build();
        List<ItemStack> rolled = table.roll(lootContext);
        List<ItemStack> items = otherHalf(block) != null ? Stream.concat(rolled.stream(), table.roll(lootContext).stream()).toList() : rolled;

        markClaimed(block, player);
        ANIMATION.play(player, block, items, () -> PlayerUtils.giveItem(player, items));
    }

    private static @Nullable String tableOf(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return null;
        if (!(block.getState() instanceof TileState tile)) return null;
        String table = tile.getPersistentDataContainer().get(Keys.REWARD_CID, PersistentDataType.STRING);
        return table == null || table.isBlank() ? null : table;
    }

    private static boolean hasClaimed(Block block, Player player) {
        Block other = otherHalf(block);
        return claimedBy(block, player) || (other != null && claimedBy(other, player));
    }

    private static boolean claimedBy(Block block, Player player) {
        if (!(block.getState() instanceof TileState tile)) return false;
        List<String> claimed = tile.getPersistentDataContainer().get(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings());
        return claimed != null && claimed.contains(player.getUniqueId().toString());
    }

    private static void markClaimed(Block block, Player player) {
        addClaim(block, player);
        Block other = otherHalf(block);
        if (other != null) addClaim(other, player);
    }

    private static void addClaim(Block block, Player player) {
        if (!(block.getState() instanceof TileState tile)) return;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        List<String> claimed = new ArrayList<>(pdc.getOrDefault(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings(), List.of()));
        String id = player.getUniqueId().toString();
        if (!claimed.contains(id)) claimed.add(id);
        pdc.set(Keys.REWARD_CLAIMED, PersistentDataType.LIST.strings(), claimed);
        tile.update();
    }

    private static @Nullable Block otherHalf(Block block) {
        if (!(block.getState() instanceof Chest chest) || !(chest.getInventory() instanceof DoubleChestInventory inventory)) return null;
        if (!(inventory.getHolder() instanceof DoubleChest doubleChest)) return null;
        if (doubleChest.getLeftSide() instanceof Chest left && !left.getBlock().equals(block)) return left.getBlock();
        if (doubleChest.getRightSide() instanceof Chest right && !right.getBlock().equals(block)) return right.getBlock();
        return null;
    }
}
