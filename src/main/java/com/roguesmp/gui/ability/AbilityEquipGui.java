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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbilityEquipGui extends BaseGui {

    private static final int MAX_PASSIVE = 14;

    private final SmpPlayer smpPlayer;
    private final AbilityTrigger trigger;
    private final List<AbilityInfo<?>> abilities = new ArrayList<>();

    public AbilityEquipGui(SmpPlayer smpPlayer, AbilityTrigger trigger) {
        super(Component.text("Chọn kĩ năng: " + trigger.name(), NamedTextColor.GREEN), 6);

        this.smpPlayer = smpPlayer;
        this.trigger = trigger;

        loadAbilities();
        sortAbilities();
    }

    private void loadAbilities() {
        PlayerData data = smpPlayer.getPlayerData();
        data.getUnlockedAbilities().forEach((id, lvl) -> {
            AbilityInfo<?> info = AbilityRegistry.getInfo(id);
            if (info == null) return;
            if (trigger == AbilityTrigger.PASSIVE) {
                if (info.trigger() == AbilityTrigger.PASSIVE) abilities.add(info);
            } else {
                if (info.trigger() == trigger) abilities.add(info);
            }
        });
    }

    private void sortAbilities() {
        PlayerData data = smpPlayer.getPlayerData();
        if (trigger == AbilityTrigger.PASSIVE) {
            List<String> passives = data.getPassiveAbilities();
            abilities.sort((a, b) -> {
                boolean aEq = passives.contains(a.id());
                boolean bEq = passives.contains(b.id());
                if (aEq && !bEq) return -1;
                if (!aEq && bEq) return 1;
                return 0;
            });
        } else {
            String equipped = data.getEquippedAbilities().get(trigger);
            abilities.sort((a, b) -> {
                if (a.id().equals(equipped)) return -1;
                if (b.id().equals(equipped)) return 1;
                return 0;
            });
        }
    }

    @Override
    public void setup() {
        PlayerData data = smpPlayer.getPlayerData();
        Map<String, Integer> unlocked = data.getUnlockedAbilities();

        int slot = 10;
        for (AbilityInfo<?> info : abilities) {
            int level = unlocked.get(info.id());
            ItemStack item = ItemStack.of(info.displayIcon());
            item.setData(DataComponentTypes.ITEM_NAME, info.displayText());
            List<Component> lore = buildLore(info, level);
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

            addButton(slot, item, event -> {
                event.setCancelled(true);
                handleClick(event, info, level);
            });

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        fillEmpty();
    }

    private List<Component> buildLore(AbilityInfo<?> info, int level) {
        PlayerData data = smpPlayer.getPlayerData();
        List<Component> lore = new ArrayList<>();
        boolean equipped;
        if (trigger == AbilityTrigger.PASSIVE) {
            equipped = data.getPassiveAbilities().contains(info.id());
        } else {
            equipped = info.id().equals(data.getEquippedAbilities().get(trigger));
        }
        if (equipped) {
            lore.add(Component.text("✔ Đang trang bị", NamedTextColor.GREEN));
        }
        lore.addAll(info.descriptionProvider().apply(smpPlayer, level));

        if (trigger == AbilityTrigger.PASSIVE) {
            lore.add(Component.empty());
            lore.add(Component.text("Click để trang bị/hủy trang bị", NamedTextColor.YELLOW));
            lore.add(Component.text(
                    "Đã chọn: "
                            + data.getPassiveAbilities().size()
                            + "/" + MAX_PASSIVE,
                    NamedTextColor.GRAY));
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
            passives.remove(info.id());
            smpPlayer.getAbilityLoadout().removePassive(info.id());
            event.getWhoClicked().sendMessage(Component.text("Đã bỏ kĩ năng ").append(info.displayText()));
        } else {
            if (passives.size() >= MAX_PASSIVE) {
                event.getWhoClicked().sendMessage(
                        Component.text("Chỉ có thể chọn tối đa "
                                + MAX_PASSIVE + " kĩ năng nội tại!", NamedTextColor.RED));
                return;
            }
            passives.add(info.id());
            Ability ability = info.factory().apply(smpPlayer, level);
            smpPlayer.getAbilityLoadout().equipPassive(ability);
            event.getWhoClicked().sendMessage(Component.text("Đã thêm kĩ năng ").append(info.displayText()));
        }

        Utils.runLater(() -> new AbilityEquipGui(smpPlayer, trigger).showInventory(event.getWhoClicked()));
    }

    private void handleActiveClick(InventoryClickEvent event, AbilityInfo<?> info, int level, PlayerData data) {
        String equippedId = data.getEquippedAbilities().get(trigger);

        if (info.id().equals(equippedId)) {
            // Unequip
            smpPlayer.getAbilityLoadout().removeActive(trigger);
            data.getEquippedAbilities().remove(trigger);

            event.getWhoClicked().sendMessage(Component.text("Đã bỏ trang bị ").append(info.displayText()));

        } else {
            // Equip
            Ability ability = info.factory().apply(smpPlayer, level);
            smpPlayer.getAbilityLoadout().equipActive(trigger, ability);
            data.getEquippedAbilities().put(trigger, info.id());

            event.getWhoClicked().sendMessage(Component.text("Đã trang bị ").append(info.displayText()));
        }

        Utils.runLater(() -> new AbilityEquipGui(smpPlayer, trigger).showInventory(event.getWhoClicked()));
    }
}
