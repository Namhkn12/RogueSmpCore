package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AbilityLoadoutGui extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final AbilityLoadout loadout;

    // Decoration Constants
    private static final ItemStack BORDER_GRAY = createDecoration(Material.GRAY_STAINED_GLASS_PANE);
    private static final ItemStack BORDER_CYAN = createDecoration(Material.CYAN_STAINED_GLASS_PANE);

    public AbilityLoadoutGui(SmpPlayer smpPlayer) {
        super(Component.text("Bảng Kĩ Năng"), 6);
        this.smpPlayer = smpPlayer;
        this.loadout = smpPlayer.getAbilityLoadout();
    }

    @Override
    public void setup() {
        clearUi();

        // 1. Fill background with deep black
        fillEmpty(FILLER_BLACK);

        // 2. Render the ability slots, spaced across the middle row (columns 1, 3, 5, 7)
        Ability[] abilities = loadout.getAbilities();
        for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++) {
            renderSlot(2, 1 + i * 2, abilities[i], i);
        }

        // 3. Navigation/Info Button
        addButton(5, 4, createInfoButton(), event -> event.setCancelled(true));
    }

    private void renderSlot(int row, int col, @Nullable Ability ability, int index) {
        ItemStack icon = (ability != null) ? createEquippedIcon(ability) : createEmptyIcon(index);
        addButton(row, col, icon, ClickHandler.openGui(new AbilitySelectionGui(smpPlayer, index)));
    }

    private ItemStack createEquippedIcon(Ability ability) {
        AbilityInfo<?> info = ability.getAbilityInfo();
        ItemStack item = ItemStack.of(info.getIcon());

        // Bold name for equipped items
        item.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName()
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("⭐ Cấp độ: " + ability.getLevel(), NamedTextColor.GOLD));
        lore.add(Component.empty());

        // Split description into lines
        lore.addAll(info.getFormattedDescription(ability.getLevel()));
        lore.add(Component.empty());
        lore.addAll(info.getFormattedActivation());

        lore.add(Component.empty());
        lore.add(Utils.text("Click để thay đổi kĩ năng", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createEmptyIcon(int index) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("➕ Ô Trống", NamedTextColor.WHITE));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Vị trí: #" + (index + 1), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Chưa có kĩ năng nào được trang bị.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Click để trang bị", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createInfoButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Hướng Dẫn", NamedTextColor.YELLOW, TextDecoration.BOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("• Bạn có " + AbilityLoadout.SLOT_COUNT + " ô kĩ năng.", NamedTextColor.GRAY),
                Utils.text("• Mỗi kĩ năng có cách kích hoạt riêng: bằng tổ hợp hành động hoặc tự động.", NamedTextColor.GRAY)
        )));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
