package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AbilityDetailGui extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final AbilityInfo<?> info;
    private final int level;

    public AbilityDetailGui(SmpPlayer smpPlayer, AbilityInfo<?> info, int level) {
        super(Component.text("Chi tiết kĩ năng"), 3);
        this.smpPlayer = smpPlayer;
        this.info = info;
        this.level = level;
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_BLACK);

        // Icon kĩ năng hiện tại
        ItemStack icon = ItemStack.of(info.getIcon());
        icon.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName());
        List<Component> information = new ArrayList<>();
        information.add(Utils.text("Cấp độ hiện tại: " + level, NamedTextColor.GRAY));
        information.add(Component.empty());
        information.addAll(info.getFormattedDescription(level));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore(information));
        addItem(1, 2, icon);

        // Nút Nâng cấp
        ItemStack actionButton;
        boolean isMax = !info.hasNextLevel(level);
        if (isMax) {
            // Nút khi đã đạt cấp tối đa
            actionButton = ItemStack.of(Material.NETHER_STAR);
            actionButton.setData(DataComponentTypes.ITEM_NAME, Component.text("Cấp tối đa", NamedTextColor.GOLD, TextDecoration.BOLD));
            actionButton.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                    Utils.text("Kĩ năng đã đạt cấp tối đa.", NamedTextColor.GRAY)
            )));

            // Thêm action thông báo hoặc cancel
            addButton(1, 6, actionButton, event -> {
                event.setCancelled(true);
                smpPlayer.sendMessage(Component.text("Kĩ năng này đã đạt cấp độ tối đa!", NamedTextColor.YELLOW));
            });
        } else {
            // Nút khi vẫn còn có thể nâng cấp
            actionButton = ItemStack.of(Material.ANVIL);
            actionButton.setData(DataComponentTypes.ITEM_NAME, Component.text("Nâng cấp kĩ năng", NamedTextColor.GREEN, TextDecoration.BOLD));
            actionButton.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                    Utils.text("Nhấn để xem thông tin nâng cấp.", NamedTextColor.GRAY)
            )));

            addButton(1, 6, actionButton, event -> {
                event.setCancelled(true);
                new AbilityUpgradeGui(smpPlayer, info, level).showInventory(event.getWhoClicked());
            });
        }

        // Quay lại danh sách
        addButton(2, 0, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            new AbilityCatalogue(smpPlayer).showInventory(event.getWhoClicked());
        });
    }
}
