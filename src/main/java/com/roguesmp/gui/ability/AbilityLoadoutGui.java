package com.roguesmp.gui.ability;

import com.roguesmp.constant.AbilityTrigger;
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

import java.util.ArrayList;
import java.util.List;

public class AbilityLoadoutGui extends BaseGui {

    private static final List<Integer> ACTIVE_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16);

    private static final List<Integer> PASSIVE_SLOTS = List.of(28,29,30,31,32,33,34,37,38,39,40,41,42,43);

    private final ItemStack noAbilItem;
    private final ItemStack filler;
    private final ItemStack activeHeader;
    private final ItemStack passiveHeader;

    private final SmpPlayer player;

    public AbilityLoadoutGui(SmpPlayer player) {
        super(Component.text("Bộ kĩ năng"), 6);
        this.player = player;

        noAbilItem = ItemStack.of(Material.GRAY_DYE);
        noAbilItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Ô kĩ năng trống", NamedTextColor.GRAY));
        noAbilItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Component.text("Click để chọn kĩ năng...", NamedTextColor.GRAY))));

        filler = ItemStack.of(Material.MAGENTA_STAINED_GLASS_PANE);
        filler.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        activeHeader = ItemStack.of(Material.DIAMOND_SWORD);
        activeHeader.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Kĩ năng chủ động", NamedTextColor.GREEN));
        activeHeader.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Component.text("Kích hoạt dùng các Trigger", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false),
                Component.text("Chuột trái/Chuột phải v.v...", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        )));

        passiveHeader = ItemStack.of(Material.BEACON);
        passiveHeader.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Kĩ năng bị động", NamedTextColor.LIGHT_PURPLE));

        passiveHeader.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Component.text("Luôn kích hoạt hoặc cần một số điều kiện đặc biệt để kích hoạt", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Đã trang bị: " + player.getPlayerData().getPassiveAbilities().size() + "/14", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        )));
    }

    @Override
    public void setup() {
        setupActiveSlots();
        setupPassiveAbilities();

        fillEmpty(filler);

        ItemStack divider = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE);
        divider.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        for (int i = 18; i <= 26; i++) {
            addItem(i, divider);
        }

        addItem(4, activeHeader);
        addItem(22, passiveHeader);
    }

    private void setupActiveSlots() {
        var loadout = player.getAbilityLoadout();

        for (AbilityTrigger abilityTrigger : AbilityTrigger.values()) {
            if (abilityTrigger == AbilityTrigger.PASSIVE) continue;
            int slot = ACTIVE_SLOTS.get(abilityTrigger.ordinal());
            Ability ability = loadout.getActiveAbilities().get(abilityTrigger);
            ItemStack item;
            if (ability == null) {
                ItemStack clone = noAbilItem.clone();
                List<Component> lore = new ArrayList<>(clone.getData(DataComponentTypes.LORE).lines());
                lore.add(Utils.text("Trigger: ", NamedTextColor.GRAY).append(abilityTrigger.simpleName().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                clone.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
                item = clone;
            } else {
                AbilityInfo<?> info = ability.getAbilityInfo();
                item = ItemStack.of(info.displayIcon());
                item.setData(DataComponentTypes.ITEM_NAME, info.displayText());
                int level = player.getPlayerData().getUnlockedAbilities().getOrDefault(info.id(), 1);

                List<Component> lore = new ArrayList<>();
                lore.addAll(info.descriptionProvider().apply(player, level));
                lore.add(Component.empty());
                lore.add(Component.text("Click để chọn kĩ năng cho trigger ").append(abilityTrigger.simpleName()).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));

                item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
            }

            item.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            item.unsetData(DataComponentTypes.FIREWORKS);

            addButton(slot, item, click -> {
                click.setCancelled(true);
                click.getWhoClicked().sendMessage(Component.text("Chọn ability để gắn vào trigger " + abilityTrigger));
                Utils.runLater(() -> new AbilityEquipGui(player, abilityTrigger).showInventory(click.getWhoClicked()));
            });
        }
    }

    private void setupPassiveAbilities() {
        AbilityLoadout loadout = player.getAbilityLoadout();
        List<Ability> passives = loadout.getPassiveAbilities();
        for (int i = 0; i < PASSIVE_SLOTS.size(); i++) {
            int slot = PASSIVE_SLOTS.get(i);
            if (i < passives.size()) {
                Ability ability = passives.get(i);
                AbilityInfo<?> info = ability.getAbilityInfo();
                ItemStack item = ItemStack.of(info.displayIcon());
                item.setData(DataComponentTypes.ITEM_NAME, info.displayText());
                int level = player.getPlayerData().getUnlockedAbilities().getOrDefault(info.id(), 1);

                List<Component> lore = info.descriptionProvider().apply(player, level);
                lore.add(Utils.text("Click để thay đổi", NamedTextColor.GRAY));
                item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

                item.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                item.unsetData(DataComponentTypes.FIREWORKS);

                addButton(slot, item, ClickHandler.openGui(new AbilityEquipGui(player, AbilityTrigger.PASSIVE)));
            } else {
                ItemStack clone = noAbilItem.clone();

                clone.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                clone.unsetData(DataComponentTypes.FIREWORKS);

                addButton(slot, clone, ClickHandler.openGui(new AbilityEquipGui(player, AbilityTrigger.PASSIVE)));
            }
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
