package com.roguesmp.dungeon_v2.actor.command;

import com.roguesmp.dungeon_v2.data.definition.loot.LootContext;
import com.roguesmp.dungeon_v2.dto.loot.LootRules;
import com.roguesmp.dungeon_v2.service.ILootService;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class LootTableCommand {

    private final ILootService lootService;

    public LootTableCommand(ILootService lootService) {
        this.lootService = lootService;
    }

    public void register() {
        new CommandAPICommand("rloottable")

                // /rloottable treasure give
                .withSubcommand(
                        new CommandAPICommand("treasure")
                                .withSubcommand(
                                        new CommandAPICommand("give")
                                                .executesPlayer((player, args) -> {
                                                    player.getInventory().addItem(buildTreasureChestItem());
                                                    player.sendMessage("You received a Treasure Chest.");
                                                })
                                )
                )

                // /rloottable roll <tableId>
                .withSubcommand(
                        new CommandAPICommand("roll")
                                .withArguments(new GreedyStringArgument("tableId"))
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");
                                    rollAndGive(player, tableId, LootContext.builder().build());
                                })
                )

                // /rloottable rollbonus <tableId> <bonusModifier>
                .withSubcommand(
                        new CommandAPICommand("rollbonus")
                                .withArguments(
                                        new NamespacedKeyArgument("tableId"),
                                        new DoubleArgument("bonusModifier", 0.0)
                                )
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");
                                    double bonus = (double) args.get("bonusModifier");

                                    LootContext ctx = LootContext.builder()
                                            .addRule(new LootRules.Fixed(bonus))
                                            .build();

                                    rollAndGive(player, tableId, ctx);
                                })
                )

                // /rloottable check <tableId>
                .withSubcommand(
                        new CommandAPICommand("check")
                                .withArguments(new GreedyStringArgument("tableId"))
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");

                                    if (!lootService.exists(tableId)) {
                                        player.sendMessage("§c[LootTable] Not found: §f" + tableId);
                                        return;
                                    }

                                    player.sendMessage("§a[LootTable] Found: §f" + tableId);
                                })
                )

                .register();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void rollAndGive(Player player, String tableId, LootContext ctx) {
        if (!lootService.exists(tableId)) {
            player.sendMessage("§c[LootTable] Not found: §f" + tableId);
            return;
        }

        List<ItemStack> items = lootService.roll(tableId, ctx);

        if (items.isEmpty()) {
            player.sendMessage("§e[LootTable] Rolled §f" + tableId + " §e— no items (empty roll)");
            return;
        }

        player.sendMessage("§a[LootTable] Rolled §f" + tableId + " §a— §f" + items.size() + " §aitem(s):");
        for (ItemStack item : items) {
            player.sendMessage("  §7- §f" + item.getType().name() + " §7x" + item.getAmount());
            player.getInventory().addItem(item);
        }
    }

    private ItemStack buildTreasureChestItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("Treasure Chest");
        meta.getPersistentDataContainer().set(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING, "dungeon");

        item.setItemMeta(meta);
        return item;
    }
}