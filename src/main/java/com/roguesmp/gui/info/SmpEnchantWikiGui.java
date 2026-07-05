package com.roguesmp.gui.info;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SmpEnchantWikiGui extends BaseGui {

    private final Player player;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;

    public SmpEnchantWikiGui(Player player) {
        this(player, 0);
    }

    public SmpEnchantWikiGui(Player player, int page) {
        super(Component.text("Tra Cứu Phù Phép - Trang " + (page + 1), NamedTextColor.DARK_PURPLE, TextDecoration.BOLD), 6);
        this.player = player;
        this.page = page;
    }

    @Override
    public void setup() {
        clearUi();

        Enchants[] allEnchants = Enchants.values();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allEnchants.length);

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            SmpEnchant smpEnch = allEnchants[i].getEnchant();

            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }

            ItemStack icon = new ItemStack(smpEnch.getIcon() != null ? smpEnch.getIcon() : Material.ENCHANTED_BOOK);
            icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
            icon.setData(DataComponentTypes.ITEM_NAME, Component.text(smpEnch.getSimpleName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Utils.text("ID: " + smpEnch.getId(), NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());

            // Description
            lore.add(Utils.text("Hiệu ứng:", NamedTextColor.YELLOW));
            lore.add(Utils.text(" " + smpEnch.getSimpleDescription(), NamedTextColor.GRAY));
            lore.add(Component.empty());

            // Active Slots Information
            lore.add(Utils.text("Ô trang bị tương thích:", NamedTextColor.GOLD));
            if (smpEnch.getActiveSlots().size() == EquipSlot.values().length) {
                lore.add(Utils.text(" • Không giới hạn tương thích", NamedTextColor.DARK_AQUA));
            } else {
                for (EquipSlot equipSlot : smpEnch.getActiveSlots()) {
                    lore.add(Utils.text(" • " + equipSlot.getSimpleName(), NamedTextColor.AQUA));
                }
            }

            icon.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

            addButton(slot, icon, event -> event.setCancelled(true));
            slot++;
        }

        // Previous Page Button
        if (page > 0) {
            addButton(45, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                new SmpEnchantWikiGui(player, page - 1).showInventory(event.getWhoClicked());
            });
        }

        // Return Button
        ItemStack backButton = ItemStack.of(Material.BARRIER);
        backButton.setData(DataComponentTypes.ITEM_NAME, Component.text("« Quay Lại Menu Chính", NamedTextColor.RED));
        addButton(49, backButton, event -> {
            event.setCancelled(true);
            new SmpWikiMainMenuGui().showInventory(event.getWhoClicked());
        });

        // Next Page Button
        if (endIndex < allEnchants.length) {
            addButton(53, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                new SmpEnchantWikiGui(player, page + 1).showInventory(event.getWhoClicked());
            });
        }

        fillEmpty(FILLER_BLACK);
    }
}
