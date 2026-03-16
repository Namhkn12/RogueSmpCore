package com.roguesmp.player.ability.impl;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VampiricStrike extends Ability {

    public static final String ID = "vampiric_strike";

    public static final List<Double> LIFESTEAL = List.of(0.05, 0.08, 0.12, 0.16);

    public static final AbilityInfo<VampiricStrike> INFO =
            new AbilityInfo.Builder<VampiricStrike>()
                    .id(ID)
                    .displayText(Component.text("Vampiric Strike", NamedTextColor.DARK_RED))
                    .displayIcon(Material.REDSTONE)
                    .trigger(AbilityTrigger.PASSIVE)
                    .descriptionProvider((p,l) ->
                            List.of(Component.text(
                                    "Hút " + (int)(LIFESTEAL.get(l-1)*100) + "% máu khi gây sát thương",
                                    NamedTextColor.GRAY)))
                    .factory(VampiricStrike::new)
                    .build();

    public VampiricStrike(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {

        double heal = event.getFinalDamage() * LIFESTEAL.get(level - 1);

        Player player = smpPlayer.getBukkitPlayer();
        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        player.heal(Math.min(maxHp, player.getHealth() + heal), EntityRegainHealthEvent.RegainReason.CUSTOM);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    @Override
    public void cast() {}
}
