package com.roguesmp.gui.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.registry.AbilityRegistry;
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
import java.util.Map;

public class AbilityEquipGui extends BaseGui {

    private static final int MAX_PASSIVE = 7;

    private static final int[] ABILITY_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    private final SmpPlayer smpPlayer;
    private final AbilityTrigger trigger;
    private final List<AbilityInfo<?>> abilities = new ArrayList<>();
    private final PlayerData data;

    private final ItemStack border;

    private int page = 0;

    public AbilityEquipGui(SmpPlayer smpPlayer, AbilityTrigger trigger) {
        super(Component.text("Chọn kĩ năng Trigger: ").append(trigger.simpleName()), 6);

        this.smpPlayer = smpPlayer;
        this.trigger = trigger;
        this.data = smpPlayer.getPlayerData();

        border = ItemStack.of(Material.PURPLE_STAINED_GLASS_PANE);
        border.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        loadAbilities();

    }

    private void loadAbilities() {

        List<AbilityInfo<?>> equipped = new ArrayList<>();
        List<AbilityInfo<?>> others = new ArrayList<>();

        String equippedActive = data.getEquippedAbilities().get(trigger);
        List<String> passives = data.getPassiveAbilities();
        Map<String, Integer> unlocked = data.getUnlockedAbilities();
        for (AbilityInfo<?> info : AbilityRegistry.getAll()) {
            if (info.trigger() != trigger) continue;
            if (!unlocked.containsKey(info.id())) continue;
            boolean isEquipped;
            if (trigger == AbilityTrigger.PASSIVE) {
                isEquipped = passives.contains(info.id());
            } else {
                isEquipped = info.id().equals(equippedActive);
            }
            if (isEquipped) {
                equipped.add(info);
            } else {
                others.add(info);
            }
        }

        abilities.clear();
        abilities.addAll(equipped);
        abilities.addAll(others);
    }

    @Override
    public void setup() {
        addBorders();

        Map<String, Integer> unlocked = data.getUnlockedAbilities();

        int start = page * ABILITY_SLOTS.length;
        int end = Math.min(start + ABILITY_SLOTS.length, abilities.size());

        int index = 0;

        for (int i = start; i < end; i++) {
            AbilityInfo<?> info = abilities.get(i);
            int level = unlocked.get(info.id());

            ItemStack item = ItemStack.of(info.displayIcon());
            item.setData(DataComponentTypes.ITEM_NAME, info.displayText());
            boolean equipped = isAbilityEquipped(info);
            List<Component> lore = buildLore(info, level, equipped);
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
            if (equipped) {
                item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            int slot = ABILITY_SLOTS[index++];

            addButton(slot, item, event -> {
                event.setCancelled(true);
                handleClick(event, info, level);
            });
        }

        setupPagination();

        fillEmpty();
    }

    private void setupPagination() {
        int maxPage = (abilities.size() - 1) / ABILITY_SLOTS.length;

        if (page > 0) {
            ItemStack prev = ItemStack.of(Material.ARROW);
            prev.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang trước", NamedTextColor.YELLOW));

            addButton(48, prev, e -> {
                page = page - 1;
                setup();
            });
        }

        if (page < maxPage) {

            ItemStack next = ItemStack.of(Material.ARROW);
            next.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang sau", NamedTextColor.YELLOW));

            addButton(50, next, e -> {
                page = page + 1;
                setup();
            });
        }

        ItemStack returnGui = ItemStack.of(Material.WRITTEN_BOOK);
        returnGui.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay về Loadout", NamedTextColor.GREEN));
        addButton(5, 0, returnGui, ClickHandler.openGui(new AbilityLoadoutGui(smpPlayer)));
    }

    private void addBorders() {
        for (int i = 0; i < 9; i++) {
            addButton(i, border, ClickHandler.noAction());
            addButton(45 + i, border, ClickHandler.noAction());
        }

        for (int row = 1; row < 5; row++) {
            addButton(row * 9, border, ClickHandler.noAction());
            addButton(row * 9 + 8, border, ClickHandler.noAction());
        }
    }

    private List<Component> buildLore(AbilityInfo<?> info, int level, boolean equipped) {
        List<Component> lore = new ArrayList<>();
        if (equipped) {
            lore.add(Component.text("✔ Đang trang bị", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        lore.addAll(info.descriptionProvider().apply(smpPlayer, level));

        if (trigger == AbilityTrigger.PASSIVE) {
            lore.add(Component.empty());
            lore.add(Component.text("Click để trang bị/hủy trang bị", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(
                    "Đã chọn: "
                            + data.getPassiveAbilities().size()
                            + "/" + MAX_PASSIVE,
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    private void handleClick(InventoryClickEvent event, AbilityInfo<?> info, int level) {
        PlayerData data = smpPlayer.getPlayerData();
        if (trigger == AbilityTrigger.PASSIVE) {
            handlePassiveClick(event, info, level, data);
        } else {
            handleActiveClick(event, info, level, data);
        }
    }

    private void handlePassiveClick(InventoryClickEvent event, AbilityInfo<?> info, int level, PlayerData data) {
        List<String> passives = data.getPassiveAbilities();
        if (passives.contains(info.id())) {
            data.removePassiveAbility(info.id());
            smpPlayer.getAbilityLoadout().removePassive(info.id());
            event.getWhoClicked().sendMessage(Component.text("Đã bỏ kĩ năng ").append(info.displayText()));
        } else {
            if (passives.size() >= MAX_PASSIVE) {
                event.getWhoClicked().sendMessage(
                        Component.text("Chỉ có thể chọn tối đa "
                                + MAX_PASSIVE + " kĩ năng nội tại!", NamedTextColor.RED));
                return;
            }
            data.equipPassiveAbility(info.id());
            Ability ability = info.factory().apply(smpPlayer, level);
            smpPlayer.getAbilityLoadout().equipPassive(ability);
            event.getWhoClicked().sendMessage(Component.text("Đã thêm kĩ năng ").append(info.displayText()));
        }

        setup();
    }

    private void handleActiveClick(InventoryClickEvent event, AbilityInfo<?> info, int level, PlayerData data) {
        String equippedId = data.getEquippedAbilities().get(trigger);

        if (info.id().equals(equippedId)) {
            // Unequip
            smpPlayer.getAbilityLoadout().removeActive(trigger);
            data.removeActiveAbility(trigger);

            event.getWhoClicked().sendMessage(Component.text("Đã bỏ trang bị ").append(info.displayText()));

        } else {
            // Equip
            Ability ability = info.factory().apply(smpPlayer, level);
            smpPlayer.getAbilityLoadout().equipActive(trigger, ability);
            data.equipActiveAbility(trigger, info.id());

            event.getWhoClicked().sendMessage(Component.text("Đã trang bị ").append(info.displayText()));
        }

        Utils.runLater(() -> new AbilityLoadoutGui(smpPlayer).showInventory(event.getWhoClicked()));
    }

    private boolean isAbilityEquipped(AbilityInfo<?> info) {
        boolean equipped;
        if (trigger == AbilityTrigger.PASSIVE) {
            equipped = data.getPassiveAbilities().contains(info.id());
        } else {
            equipped = info.id().equals(data.getEquippedAbilities().get(trigger));
        }
        return equipped;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
