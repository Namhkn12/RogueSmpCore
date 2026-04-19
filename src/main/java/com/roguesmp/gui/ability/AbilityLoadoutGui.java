package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.AbilityType;
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

        // 3. Render Active Slots (Row 1, Slots 1-4)
        Ability[] actives = loadout.getAbilities(AbilityType.ACTIVE);
        for (int i = 0; i < AbilityType.ACTIVE.getMaxSlots(); i++) {
            renderSlot(1, i + 1, actives[i], AbilityType.ACTIVE, i);
        }

        // 4. Render Passive Slots (Row 3, Slots 1-4)
        Ability[] passives = loadout.getAbilities(AbilityType.PASSIVE);
        for (int i = 0; i < AbilityType.PASSIVE.getMaxSlots(); i++) {
            renderSlot(3, i + 1, passives[i], AbilityType.PASSIVE, i);
        }

        // 5. Render Lifeline Slot (Row 1, Slot 7)
        Ability[] lifeline = loadout.getAbilities(AbilityType.LIFELINE);
        renderSlot(1, 7, lifeline[0], AbilityType.LIFELINE, 0);

        // 6. Navigation/Info Button
        addButton(5, 4, createInfoButton(), event -> event.setCancelled(true));
    }

    private void renderSlot(int row, int col, @Nullable Ability ability, AbilityType type, int index) {
        ItemStack icon = (ability != null) ? createEquippedIcon(ability, type) : createEmptyIcon(type, index);
        addButton(row, col, icon, ClickHandler.openGui(new AbilitySelectionGui(smpPlayer, type, index)));
    }

    private ItemStack createEquippedIcon(Ability ability, AbilityType type) {
        AbilityInfo<?> info = ability.getAbilityInfo();
        ItemStack item = ItemStack.of(info.getIcon());

        // Bold name for equipped items
        item.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName()
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("✨ " + type.getDisplay(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, true));
        lore.add(Utils.text("⭐ Cấp độ: " + ability.getLevel(), NamedTextColor.GOLD));
        lore.add(Component.empty());

        // Split description into lines
        lore.addAll(info.getFormattedDescription(ability.getLevel()));

        lore.add(Component.empty());
        lore.add(Utils.text("Click để thay đổi kĩ năng", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createEmptyIcon(AbilityType type, int index) {
        // Different colors for empty slot types
        Material material = switch (type) {
            case ACTIVE, PASSIVE -> Material.LIME_STAINED_GLASS_PANE;
            case LIFELINE -> Material.PINK_STAINED_GLASS_PANE;
        };

        ItemStack item = new ItemStack(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("➕ Ô Trống: " + type.getDisplay(), NamedTextColor.WHITE));

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
                Utils.text("• Kĩ năng Chủ Động: Sử dụng bằng các tổ hợp hành động.", NamedTextColor.GRAY),
                Utils.text("• Kĩ năng Bị Động: Luôn tự động kích hoạt.", NamedTextColor.GRAY),
                Utils.text("• Kĩ năng Sinh Tử: Kích hoạt khi gặp nguy cấp.", NamedTextColor.GRAY)
        )));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
