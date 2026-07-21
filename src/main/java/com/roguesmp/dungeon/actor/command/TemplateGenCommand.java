package com.roguesmp.dungeon.actor.command;

import com.roguesmp.loot.context.LootContext;
import com.roguesmp.dungeon.data.definition.spawner.Spawner;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.loot.manager.LootTableManager;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.manager.SpawnerManager;
import com.roguesmp.loot.service.ILootService;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.function.Predicate;

public class TemplateGenCommand {

    private final ILootService lootService;
    private final SpawnerManager spawnerManager;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;
    private final LootTableManager lootTableManager;
    private final SchemetaManager schemetaManager;

    public TemplateGenCommand(
            ILootService lootService,
            SpawnerManager spawnerManager,
            DungeonManager dungeonManager,
            RoomManager roomManager,
            LootTableManager lootTableManager,
            SchemetaManager schemetaManager
    ) {
        this.lootService = lootService;
        this.spawnerManager = spawnerManager;
        this.dungeonManager = dungeonManager;
        this.roomManager = roomManager;
        this.lootTableManager = lootTableManager;
        this.schemetaManager = schemetaManager;
    }

    public void register() {
        registerRoot("template");
        registerRoot("templates");
    }

    private void registerRoot(String rootCommand) {
        new CommandAPICommand(rootCommand)
                .withSubcommand(buildSpawnerCommand())
                .withSubcommand(buildLootTableCommand())
                .withSubcommand(buildReloadCommand())
                .register();
    }

    private CommandAPICommand buildSpawnerCommand() {
        return new CommandAPICommand("spawner")
                .withSubcommand(
                        new CommandAPICommand("get")
                                .withArguments(
                                        new StringArgument("templateId")
                                                .withRequirement(inBuildingWorld())
                                                .replaceSuggestions(ArgumentSuggestions.strings(
                                                        info -> spawnerManager.getAllIds().toArray(String[]::new)
                                                ))
                                )
                                .executesPlayer((player, args) -> {
                                    String templateId = (String) args.get("templateId");
                                    Spawner template = spawnerManager.getById(templateId);

                                    if (template == null) {
                                        player.sendMessage("§c[Template] Spawner template not found: §f" + templateId);
                                        return;
                                    }

                                    player.getInventory().addItem(
                                            buildSpawnerItem(template.getId(), resolveSpawnerDisplayName(template))
                                    );
                                    player.sendMessage("§a[Template] Given spawner template: §f" + templateId);
                                })
                );
    }

    private CommandAPICommand buildLootTableCommand() {
        return new CommandAPICommand("loottable")
                .withSubcommand(
                        new CommandAPICommand("treasure")
                                .withRequirement(inBuildingWorld())
                                .withSubcommand(
                                        new CommandAPICommand("give")
                                                .withArguments(
                                                        treasureTableIdArgument("tableId")
                                                )
                                                .executesPlayer((player, args) -> {

                                                    String tableId = (String) args.get("tableId");

                                                    if (!tableId.equals("dungeon")
                                                            && !lootTableManager.exists(tableId)) {

                                                        player.sendMessage(
                                                                "§c[LootTable] Not found: §f" + tableId
                                                        );
                                                        return;
                                                    }

                                                    player.getInventory().addItem(
                                                            buildTreasureChestItem(tableId)
                                                    );

                                                    player.sendMessage(
                                                            "§aYou received a Treasure Chest for: §f"
                                                                    + tableId
                                                    );
                                                })
                                )
                )
                .withSubcommand(
                        new CommandAPICommand("roll")
                                .withRequirement(inBuildingWorld())
                                .withArguments(lootTableIdArgument("tableId"))
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");
                                    rollAndGive(player, tableId, LootContext
                                            .builder(PlayerManager.getInstance().getSmpPlayer(player))
                                            .origin(LootOrigin.COMMAND, player)
                                            .build());
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("rollbonus")
                                .withRequirement(inBuildingWorld())
                                .withArguments(
                                        new DoubleArgument("bonusModifier", 0.0),
                                        lootTableIdArgument("tableId")  // GreedyString ở cuối
                                )
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");
                                    double bonus = (double) args.get("bonusModifier");
                                    LootContext ctx = LootContext
                                            .builder(PlayerManager.getInstance().getSmpPlayer(player))
                                            .origin(LootOrigin.COMMAND, player)
                                            .addModifier("command", bonus)
                                            .build();
                                    rollAndGive(player, tableId, ctx);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("check")
                                .withRequirement(inBuildingWorld())
                                .withArguments(lootTableIdArgument("tableId"))
                                .executesPlayer((player, args) -> {
                                    String tableId = (String) args.get("tableId");
                                    if (!lootService.exists(tableId)) {
                                        player.sendMessage("§c[LootTable] Not found: §f" + tableId);
                                        return;
                                    }
                                    player.sendMessage("§a[LootTable] Found: §f" + tableId);
                                })
                );
    }

    private CommandAPICommand buildReloadCommand() {
        return new CommandAPICommand("reload")
                .withRequirement(inBuildingWorld())
                .executes((sender, args) -> {
                    sender.sendMessage(reloadAllTemplates());
                })
                .withSubcommand(new CommandAPICommand("all")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadAllTemplates());
                        }))
                .withSubcommand(new CommandAPICommand("spawner")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadSpawnerTemplates());
                        }))
                .withSubcommand(new CommandAPICommand("dungeon")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadDungeonTemplates());
                        }))
                .withSubcommand(new CommandAPICommand("room")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadRoomTemplates());
                        }))
                .withSubcommand(new CommandAPICommand("loottable")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadLootTableTemplates());
                        }))
                .withSubcommand(new CommandAPICommand("schemeta")
                        .withRequirement(inBuildingWorld())
                        .executes((sender, args) -> {
                            sender.sendMessage(reloadSchemetaTemplates());
                        }));
    }

    private String reloadAllTemplates() {
        schemetaManager.loadAll();
        spawnerManager.load();
        roomManager.loadAll();
        dungeonManager.loadAll();
        lootTableManager.load();

        return "§a[Template] Reloaded all templates. §7"
                + "schemeta=" + schemetaManager.getAll().size()
                + ", spawner=" + spawnerManager.getAllIds().size()
                + ", room=" + roomManager.getAllIds().size()
                + ", dungeon=" + dungeonManager.getAllIds().size()
                + ", loottable=" + lootTableManager.getCacheSize();
    }

    private String reloadSpawnerTemplates() {
        spawnerManager.load();
        return "§a[Template] Reloaded spawner templates: §f" + spawnerManager.getAllIds().size();
    }

    private String reloadDungeonTemplates() {
        dungeonManager.loadAll();
        return "§a[Template] Reloaded dungeon templates: §f" + dungeonManager.getAllIds().size();
    }

    private String reloadRoomTemplates() {
        roomManager.loadAll();
        return "§a[Template] Reloaded room templates: §f" + roomManager.getAllIds().size();
    }

    private String reloadLootTableTemplates() {
        lootTableManager.load();
        return "§a[Template] Reloaded loot table templates: §f" + lootTableManager.getCacheSize();
    }

    private String reloadSchemetaTemplates() {
        schemetaManager.loadAll();
        return "§a[Template] Reloaded schemeta templates: §f" + schemetaManager.getAll().size();
    }

    private Argument<String> lootTableIdArgument(String nodeName) {
        return new GreedyStringArgument(nodeName)
                .replaceSuggestions(ArgumentSuggestions.strings(
                        info -> lootTableManager.getAllTables().keySet()
                                .stream()
                                .sorted()
                                .toArray(String[]::new)
                ));
    }

    private String resolveSpawnerDisplayName(Spawner spawner) {
        if (spawner.getName() != null && !spawner.getName().isBlank()) {
            return spawner.getName();
        }
        return spawner.getId();
    }

    private void rollAndGive(Player player, String tableId, LootContext ctx) {
        if (!lootService.exists(tableId)) {
            player.sendMessage("§c[LootTable] Not found: §f" + tableId);
            return;
        }
        List<ItemStack> items = lootService.roll(tableId, ctx);
        if (items.isEmpty()) {
            player.sendMessage("§e[LootTable] Rolled §f" + tableId + " §e- no items (empty roll)");
            return;
        }
        player.sendMessage("§a[LootTable] Rolled §f" + tableId + " §a- §f" + items.size() + " §aitem(s):");
        for (ItemStack item : items) {
            player.sendMessage("  §7- §f" + item.getType().name() + " §7x" + item.getAmount());
            player.getInventory().addItem(item);
        }
    }

    private ItemStack buildSpawnerItem(String templateId, String displayName) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        meta.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, templateId);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTreasureChestItem(String rewardId) {
        ItemStack item = new ItemStack(Material.CHEST);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("Treasure Chest");

        meta.getPersistentDataContainer().set(
                NameSpaceKeys.REWARD_CID_KEY,
                PersistentDataType.STRING,
                rewardId
        );

        item.setItemMeta(meta);

        return item;
    }

    private Argument<String> treasureTableIdArgument(String nodeName) {
        return new GreedyStringArgument(nodeName)
                .replaceSuggestions(ArgumentSuggestions.strings(
                        info -> java.util.stream.Stream.concat(
                                java.util.stream.Stream.of("dungeon"),
                                lootTableManager.getAllTables().keySet().stream().sorted()
                        ).toArray(String[]::new)
                ));
    }

    private Predicate<CommandSender> inBuildingWorld() {
        return sender -> {
            if (!(sender instanceof Player player)) return false;
            return player.getWorld().getName().startsWith("building");
        };
    }
}
