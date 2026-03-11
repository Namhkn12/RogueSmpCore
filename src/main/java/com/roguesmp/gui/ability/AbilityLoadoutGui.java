package com.roguesmp.gui.ability;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AbilityLoadoutGui extends BaseGui {

    private static final List<Integer> ACTIVE_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16);

    private static final List<Integer> PASSIVE_SLOTS = List.of(28,29,30,31,32,33,34,37,38,39,40,41,42,43);

    private final ItemStack noAbilItem;
    private final ItemStack filler;

    private final SmpPlayer player;

    public AbilityLoadoutGui(SmpPlayer player) {
        super(Component.text("Bộ kĩ năng"), 6);
        this.player = player;

        noAbilItem = ItemStack.of(Material.GRAY_DYE);
        noAbilItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Ô kĩ năng trống", NamedTextColor.GRAY));
        noAbilItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Component.text("Click để chọn kĩ năng...", NamedTextColor.GRAY))));

        filler = ItemStack.of(Material.MAGENTA_STAINED_GLASS_PANE);
        filler.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
    }

    @Override
    public void setup() {
        setupActiveSlots();
        setupPassiveAbilities();

        fillEmpty(filler);
    }

    private void setupActiveSlots() {
        var loadout = player.getAbilityLoadout();

        for (AbilityTrigger abilityTrigger : AbilityTrigger.values()) {
            if (abilityTrigger == AbilityTrigger.PASSIVE) continue;
            int slot = ACTIVE_SLOTS.get(abilityTrigger.ordinal());
            Ability ability = loadout.getActiveAbilities().get(abilityTrigger);
            ItemStack item;
            if (ability == null) {
                item = noAbilItem;
            } else {
                AbilityInfo<?> info = ability.getAbilityInfo();
                item = ItemStack.of(info.displayIcon());
                item.setData(DataComponentTypes.ITEM_NAME, info.displayText());
                int level = player.getPlayerData().getUnlockedAbilities().getOrDefault(info.id(), 1);
                item.setData(DataComponentTypes.LORE, ItemLore.lore(info.descriptionProvider().apply(player, level)));
            }

            addButton(slot, item, click -> {
                click.setCancelled(true);
                click.getWhoClicked().sendMessage(Component.text("Chọn ability để gắn vào slot " + abilityTrigger));
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
                item.setData(DataComponentTypes.LORE, ItemLore.lore(info.descriptionProvider().apply(player, level)));
                addButton(slot, item, click -> {
                    click.setCancelled(true);
                    setup();
                });
            } else {
                addButton(slot, noAbilItem, ClickHandler.openGui(new AbilityEquipGui(player, AbilityTrigger.PASSIVE)));
            }
        }
    }

    public static void register() {
        new CommandAPICommand("loadout")
                .executesPlayer((player1, commandArguments) -> {
                    SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player1.getUniqueId());
                    if (smpPlayer == null) return;
                    new AbilityLoadoutGui(smpPlayer).showInventory(player1);
                })
                .register(RogueSmpCore.getInstance());
    }
}
