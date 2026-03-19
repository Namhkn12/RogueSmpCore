package com.roguesmp.player.ability.impl;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LightningStrike extends Ability {

    public static final String ID = "lightning_strike";
    public static final List<Integer> DAMAGES = List.of(12, 20, 30, 40);

    public static final AbilityInfo<LightningStrike> INFO =
            new AbilityInfo.Builder<LightningStrike>()
                    .id(ID)
                    .displayText(Component.text("Lightning Strike", NamedTextColor.YELLOW))
                    .displayIcon(Material.LIGHTNING_ROD)
                    .trigger(AbilityTrigger.SHIFT_RIGHT_CLICK)
                    .descriptionProvider((p,l) ->
                            List.of(Component.text(
                                    "Triệu hồi sét gây " + DAMAGES.get(l-1) + " sát thương",
                                    NamedTextColor.GRAY)))
                    .factory(LightningStrike::new)
                    .build();

    public LightningStrike(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void cast() {

        RayTraceResult result = smpPlayer.getBukkitPlayer().rayTraceEntities(15);

        if (result != null && result.getHitEntity() instanceof LivingEntity mob) {

            Location loc = mob.getLocation();

            loc.getWorld().strikeLightningEffect(loc);

            DamageUtils.damage(
                    mob,
                    smpPlayer.getBukkitPlayer(),
                    DAMAGES.get(level - 1),
                    new DamageEvent.Metadata(ID, DamageType.MAGIC)
            );
        }

        setCooldownTick(120);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
