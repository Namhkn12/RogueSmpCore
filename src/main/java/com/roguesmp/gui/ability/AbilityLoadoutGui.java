package com.roguesmp.gui.ability;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityLoadout;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AbilityLoadoutGui extends BaseGui {

    private static final List<Integer> abilitySlot = List.of(10, 11, 12, 13, 14, 15, 16);
    private static final ItemStack noAbilItem = ItemStack.of(Material.STRING);

    private final SmpPlayer player;

    public AbilityLoadoutGui(SmpPlayer player) {
        super(Component.text("Bộ kĩ năng"), 6);
        this.player = player;
    }

    @Override
    public void setup() {
        AbilityLoadout loadout = player.getAbilityLoadout();
        Map<AbilityTrigger, Ability> equipped = loadout.getEquippedAbilities();
        List<Ability> passives = loadout.getPassiveAbilities();

        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            Ability ability = equipped.get(trigger);
            ItemStack result;
            if (ability != null) {
                AbilityInfo<?> info = ability.getAbilityInfo();
                result = info.getDisplayItem().clone();
                result.setData(DataComponentTypes.ITEM_NAME, info.getDisplayText());
                result.setData(DataComponentTypes.LORE, ItemLore.lore(Collections.singletonList(info.getDescriptionProvider().apply(player, ability.getLevel()))));
            } else result = noAbilItem;
            int pos = abilitySlot.get(trigger.ordinal());
            this.addButton(pos, result, ClickHandler.noAction());
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
