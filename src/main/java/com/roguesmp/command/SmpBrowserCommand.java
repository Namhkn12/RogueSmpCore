package com.roguesmp.command;

import com.roguesmp.block.gui.BlockBrowserGui;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.gui.crafting.RecipeBrowserGui;
import com.roguesmp.gui.entitycreator.EntityBrowserGui;
import com.roguesmp.gui.loottablecreator.LootTableBrowserGui;
import com.roguesmp.gui.npccreator.NpcBrowserGui;
import com.roguesmp.gui.spawner.SpawnerBrowserGui;
import com.roguesmp.gui.tageditor.TagEditorGui;
import com.roguesmp.permission.Permissions;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;

public class SmpBrowserCommand {
    public static void register() {

        CommandAPICommand itemCommand = new CommandAPICommand("item")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((sender, args) -> {
                    new ItemBrowser(query(args)).showInventory(sender);
                });

        CommandAPICommand recipeCommand = new CommandAPICommand("recipe")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((sender, args) -> {
                    new RecipeBrowserGui(sender, query(args)).showInventory(sender);
                });

        CommandAPICommand entityCommand = new CommandAPICommand("entity")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((sender, args) -> {
                    new EntityBrowserGui(sender, query(args)).showInventory(sender);
                });

        CommandAPICommand lootCommand = new CommandAPICommand("loot")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((player, args) -> {
                    new LootTableBrowserGui(player, query(args)).showInventory(player);
                });

        CommandAPICommand npcCommand = new CommandAPICommand("npc")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((player, args) -> {
                    new NpcBrowserGui(player, query(args)).showInventory(player);
                });

        CommandAPICommand spawnerCommand = new CommandAPICommand("spawner")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((player, args) -> {
                    new SpawnerBrowserGui(player, query(args)).showInventory(player);
                });

        CommandAPICommand tagCommand = new CommandAPICommand("tag")
                .executesPlayer((player, args) -> {
                    TagEditorGui.open(player);
                });

        CommandAPICommand blockCommand = new CommandAPICommand("block")
                .withOptionalArguments(new GreedyStringArgument("query"))
                .executesPlayer((player, args) -> {
                    new BlockBrowserGui(player, query(args)).showInventory(player);
                });

        new CommandAPICommand("smpbrowser")
                .withPermission(Permissions.CONTENT.node())
                .withSubcommand(itemCommand)
                .withSubcommand(recipeCommand)
                .withSubcommand(entityCommand)
                .withSubcommand(lootCommand)
                .withSubcommand(npcCommand)
                .withSubcommand(spawnerCommand)
                .withSubcommand(tagCommand)
                .withSubcommand(blockCommand)
                .register();
    }

    private static String query(CommandArguments args) {
        return (String) args.get("query");
    }
}
