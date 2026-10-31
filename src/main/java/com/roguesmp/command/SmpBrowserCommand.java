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

public class SmpBrowserCommand {
    public static void register() {

        CommandAPICommand itemCommand = new CommandAPICommand("item")
                .executesPlayer((sender, args) -> {
                    new ItemBrowser().showInventory(sender);
                });

        CommandAPICommand recipeCommand = new CommandAPICommand("recipe")
                .executesPlayer((sender, args) -> {
                    new RecipeBrowserGui(sender).showInventory(sender);
                });

        CommandAPICommand entityCommand = new CommandAPICommand("entity")
                .executesPlayer((sender, args) -> {
                    new EntityBrowserGui(sender).showInventory(sender);
                });

        CommandAPICommand lootCommand = new CommandAPICommand("loot")
                .executesPlayer((player, args) -> {
                    new LootTableBrowserGui(player).showInventory(player);
                });

        CommandAPICommand npcCommand = new CommandAPICommand("npc")
                .executesPlayer((player, args) -> {
                    new NpcBrowserGui(player).showInventory(player);
                });

        CommandAPICommand spawnerCommand = new CommandAPICommand("spawner")
                .executesPlayer((player, args) -> {
                    new SpawnerBrowserGui(player).showInventory(player);
                });

        CommandAPICommand tagCommand = new CommandAPICommand("tag")
                .executesPlayer((player, args) -> {
                    TagEditorGui.open(player);
                });

        CommandAPICommand blockCommand = new CommandAPICommand("block")
                .executesPlayer((player, args) -> {
                    new BlockBrowserGui(player).showInventory(player);
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
}
