package com.roguesmp.dungeon_v2.actor.command;

import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class TemplateGenCommand {

    public void register() {
        new CommandAPICommand("templates")
                .withSubcommand(
                        new CommandAPICommand("spawner")
                                .withSubcommand(
                                        new CommandAPICommand("get")
                                                .withSubcommand(
                                                        new CommandAPICommand("archer_post").executesPlayer((player, args) -> {
                                                            player.getInventory().addItem(buildSpawnerItem("spawner_archer_post", "Archer Post"));
                                                            player.sendMessage("Give Archer Post Spawner");
                                                        })
                                                )
                                                .withSubcommand(
                                                        new CommandAPICommand("brute_patrol").executesPlayer((player, args) -> {
                                                            player.getInventory().addItem(buildSpawnerItem("spawner_brute_patrol", "Brute Patrol"));
                                                            player.sendMessage("Give Brute Patrol Spawner");
                                                        })
                                                )
                                                .withSubcommand(
                                                        new CommandAPICommand("test").executesPlayer((player, args) -> {
                                                            player.getInventory().addItem(buildSpawnerItem("spawner_test_01", "Test Spawner"));
                                                            player.sendMessage("Give Test Spawner");
                                                        })
                                                )
                                )
                )
                .register();
    }

    private ItemStack buildSpawnerItem(String templateId, String displayName) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        meta.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, templateId);
        item.setItemMeta(meta);
        return item;
    }
}