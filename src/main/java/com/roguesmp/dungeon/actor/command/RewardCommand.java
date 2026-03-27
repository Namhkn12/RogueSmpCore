package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.utils.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RewardCommand {

    public void register() {
        new CommandAPICommand("reward")
                .withSubcommand(
                        new CommandAPICommand("give")
                                .executesPlayer((player, args) -> {
                                    if (player == null) return;

                                    player.getInventory().addItem(buildRewardChestItem());
                                    player.sendMessage(ChatColor.GOLD + "Bạn nhận được 1 " + ChatColor.YELLOW + "Template Reward Chest" + ChatColor.GOLD + "!");
                                })
                )
                .executes((sender, args) -> {
                    sender.sendMessage("Usage: /reward <give>");
                })
                .register();
    }

    private ItemStack buildRewardChestItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Reward Chest");
        meta.setLore(List.of(
                ChatColor.GRAY + "Đặt xuống để tạo rương phần thưởng.",
                ChatColor.DARK_GRAY + "Người chơi sẽ nhận item khi tương tác."
        ));

        meta.getPersistentDataContainer().set(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}