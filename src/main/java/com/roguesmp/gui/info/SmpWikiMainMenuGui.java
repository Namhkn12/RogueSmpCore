package com.roguesmp.gui.info;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SmpWikiMainMenuGui extends BaseGui {

    public SmpWikiMainMenuGui() {
        super(Component.text("Thư Viện Chỉ Số & Bùa Phép", NamedTextColor.DARK_GRAY, TextDecoration.BOLD), 3);
    }

    @Override
    public void setup() {
        clearUi();

        // 1. Attributes Category Button
        ItemStack attributeIcon = ItemStack.of(Material.BOOKSHELF);
        attributeIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("DANH SÁCH CHỈ SỐ", NamedTextColor.GOLD, TextDecoration.BOLD));
        attributeIcon.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Xem thông tin chi tiết về các", NamedTextColor.GRAY),
                Utils.text("chỉ số chiến đấu, phòng thủ trên server.", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("» Nhấn vào để xem «", NamedTextColor.YELLOW))));

        addButton(11, attributeIcon, event -> {
            event.setCancelled(true);
            new SmpAttributeWikiGui((Player) event.getWhoClicked()).showInventory(event.getWhoClicked());
        });

        // 2. Enchantments Category Button
        ItemStack enchantIcon = new ItemStack(Material.ENCHANTING_TABLE);
        enchantIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("DANH SÁCH PHÙ PHÉP", NamedTextColor.AQUA, TextDecoration.BOLD));
        enchantIcon.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Xem thông tin chi tiết về các", NamedTextColor.GRAY),
                Utils.text("bùa phép tùy chọn có trên trang bị.", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("» Nhấn vào để xem «", NamedTextColor.YELLOW)
        )));

        addButton(15, enchantIcon, event -> {
            event.setCancelled(true);
            new SmpEnchantWikiGui((Player) event.getWhoClicked()).showInventory(event.getWhoClicked());
        });

        // Frame and fillers
        fillEmpty();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    public static void registerCommands() {
        new CommandAPICommand("wiki")
                .withAliases("smpwiki", "chiso", "phuphap")
                .withShortDescription("Mở thư viện tra cứu chỉ số và bùa phép hệ thống.")
                .executesPlayer((player, args) -> {
                    new SmpWikiMainMenuGui().showInventory(player);
                })
                .register();
    }
}
