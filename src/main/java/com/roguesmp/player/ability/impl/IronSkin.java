package com.roguesmp.player.ability.impl;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IronSkin extends Ability {

    public static final String ID = "iron_skin";

    public static final List<Double> REDUCTION = List.of(0.08,0.12,0.16,0.20);

    public static final AbilityInfo<IronSkin> INFO =
            new AbilityInfo.Builder<IronSkin>()
                    .id(ID)
                    .displayText(Component.text("Iron Skin", NamedTextColor.GRAY))
                    .displayIcon(Material.IRON_CHESTPLATE)
                    .trigger(AbilityTrigger.PASSIVE)
                    .descriptionProvider((p,l) ->
                            List.of(Component.text(
                                    "Giảm " + (int)(REDUCTION.get(l-1)*100) + "% sát thương nhận",
                                    NamedTextColor.GRAY)))
                    .factory(IronSkin::new)
                    .build();

    public IronSkin(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void onHurt(DamageEvent event) {
        event.addDamageModifier(1 - REDUCTION.get(level - 1), DamageOperation.MORE_FINAL);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    @Override
    public void cast() {}
}
