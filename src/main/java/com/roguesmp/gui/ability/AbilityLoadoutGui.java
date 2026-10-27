package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.classes.ClassSelectionGui;
import com.roguesmp.player.classes.PlayerClass;
import net.kyori.adventure.key.Key;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
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

    private static final ItemStack BORDER_GRAY = createDecoration(Material.GRAY_STAINED_GLASS_PANE);
    private static final ItemStack BORDER_CYAN = createDecoration(Material.CYAN_STAINED_GLASS_PANE);
    private static final int BORDER_TOP_ROW = 1;
    private static final int ICON_ROW = 2;
    private static final int ACTIVATION_ROW = 3;
    private static final int BORDER_BOTTOM_ROW = 4;
    private static final int NAV_ROW = 5;

    public AbilityLoadoutGui(SmpPlayer smpPlayer) {
        super(Component.text("Bảng Kĩ Năng", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), 6);
        this.smpPlayer = smpPlayer;
        this.loadout = smpPlayer.getAbilityLoadout();
    }

    @Override
    public void setup() {
        clearUi();

        Ability[] abilities = loadout.getAbilities();
        for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++) {
            renderSlot(1 + i * 2, abilities[i], i);
        }

        addBackButton(getSlot(NAV_ROW, 0));
        addButton(NAV_ROW, 4, createInfoButton(), ClickHandler.noAction());
        addButton(NAV_ROW, 8, createChangeClassButton(), event -> {
            event.setCancelled(true);
            BaseGui classSelection = new ClassSelectionGui(smpPlayer).withBackAction(getBackTitle(), getBackAction());
            Utils.runLater(() -> classSelection.showInventory(event.getWhoClicked()));
        });
        fillEmpty(FILLER_BLACK);
    }

    private ItemStack createChangeClassButton() {
        PlayerClass playerClass = smpPlayer.getPlayerClass();
        ItemStack item = ItemStack.of(Material.PAPER);
        if (playerClass != null) item.setData(DataComponentTypes.ITEM_MODEL, Key.key(playerClass.getIcon()));
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Đổi Lớp Nhân Vật", NamedTextColor.AQUA, TextDecoration.BOLD));
        Component current = playerClass == null ? Utils.text("Chưa chọn", NamedTextColor.RED) : playerClass.getFormattedDisplayName();
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Lớp hiện tại: ", NamedTextColor.GRAY).append(current),
                Component.empty(),
                Utils.text("Click để đổi lớp", NamedTextColor.YELLOW))));
        return item;
    }

    private void renderSlot(int col, @Nullable Ability ability, int index) {
        ItemStack border = ability != null ? BORDER_CYAN : BORDER_GRAY;
        addButton(BORDER_TOP_ROW, col, border, ClickHandler.noAction());
        addButton(BORDER_BOTTOM_ROW, col, border, ClickHandler.noAction());
        addButton(ACTIVATION_ROW, col, createActivationIcon(ability, index), ClickHandler.noAction());

        ItemStack icon = (ability != null) ? createEquippedIcon(ability) : createEmptyIcon(index);
        addButton(ICON_ROW, col, icon, ClickHandler.openGui(new AbilitySelectionGui(smpPlayer, index)));
    }

    private ItemStack createActivationIcon(@Nullable Ability ability, int index) {
        ItemStack item = ItemStack.of(ability != null ? Material.LIGHTNING_ROD : Material.GRAY_DYE);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Ô Kĩ Năng #" + (index + 1), NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        if (ability == null) {
            lore.add(Utils.text("Chưa trang bị", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(Utils.text("Cách kích hoạt:", NamedTextColor.GRAY));
            lore.addAll(ability.getAbilityInfo().getFormattedActivation());
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createEquippedIcon(Ability ability) {
        AbilityInfo<?> info = ability.getAbilityInfo();
        ItemStack item = ItemStack.of(info.getIcon());

        item.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName()
                .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("⭐ Cấp độ: " + ability.getLevel(), NamedTextColor.GOLD));
        lore.add(Component.empty());

        lore.addAll(info.getFormattedDescription(ability.getLevel()));
        lore.add(Component.empty());
        lore.add(Utils.text("Click để thay đổi kĩ năng", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createEmptyIcon(int index) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("➕ Ô Trống", NamedTextColor.WHITE));

        List<Component> lore = new ArrayList<>();
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
