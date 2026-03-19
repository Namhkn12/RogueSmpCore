package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.ultis.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerCommand {

    public void register(){
        new CommandAPICommand("rspawner")
                .withSubcommand(
                        new CommandAPICommand("get")
                                .executesPlayer((player, commandArguments) -> {
                                    String templateId = "spawner_test_01";

                                    ItemStack item = new ItemStack(Material.SPAWNER);
                                    ItemMeta meta = item.getItemMeta();

                                    meta.displayName(Component.text("Test Spawner"));
                                    meta.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, templateId);

                                    item.setItemMeta(meta);
                                    player.getInventory().addItem(item);
                                    player.sendMessage("Give Spawner");
                                })
                )
                .register();
    }
}
