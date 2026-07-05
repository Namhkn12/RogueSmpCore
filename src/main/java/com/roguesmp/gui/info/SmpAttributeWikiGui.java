package com.roguesmp.gui.info;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
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

public class SmpAttributeWikiGui extends BaseGui {

    private final Player player;
    private final int page;
    private static final int ITEMS_PER_PAGE = 28;

    public SmpAttributeWikiGui(Player player) {
        this(player, 0);
    }

    public SmpAttributeWikiGui(Player player, int page) {
        super(Component.text("Tra Cứu Chỉ Số - Trang " + (page + 1), NamedTextColor.DARK_AQUA, TextDecoration.BOLD), 6);
        this.player = player;
        this.page = page;
    }

    @Override
    public void setup() {
        clearUi();

        Attributes[] allAttributes = Attributes.values();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allAttributes.length);

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            SmpAttribute smpAttr = allAttributes[i].getAttribute();

            // Skip the edge/border columns (Column 0 and Column 8)
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }

            ItemStack icon = ItemStack.of(smpAttr.getIcon());
            icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
            icon.setData(DataComponentTypes.ITEM_NAME, Component.text(smpAttr.getSimpleName(), NamedTextColor.GREEN, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Utils.text("ID: " + smpAttr.getId(), NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());

            lore.add(Utils.text("Mô tả:", NamedTextColor.YELLOW));
            lore.add(Utils.text(" " + smpAttr.getSimpleDescription(), NamedTextColor.GRAY));

            icon.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

            addButton(slot, icon, event -> event.setCancelled(true));
            slot++;
        }

        // Previous Page Button
        if (page > 0) {
            addButton(45, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                new SmpAttributeWikiGui(player, page - 1).showInventory(event.getWhoClicked());
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
        if (endIndex < allAttributes.length) {
            addButton(53, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                new SmpAttributeWikiGui(player, page + 1).showInventory(event.getWhoClicked());
            });
        }

        fillEmpty(BaseGui.FILLER_BLACK);
    }
}
