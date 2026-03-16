package com.roguesmp.player.ability.impl;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.Hitbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FrostNova extends Ability {

    public static final String ID = "frost_nova";

    public static final AbilityInfo<FrostNova> INFO =
            new AbilityInfo.Builder<FrostNova>()
                    .id(ID)
                    .displayText(Component.text("Frost Nova", NamedTextColor.AQUA).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .displayIcon(Material.ICE)
                    .trigger(AbilityTrigger.SWAP)
                    .descriptionProvider((p,l) ->
                            List.of(Component.text(
                                    "Đóng băng kẻ địch xung quanh",
                                    NamedTextColor.GRAY)))
                    .factory(FrostNova::new)
                    .build();

    public FrostNova(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void cast() {

        Location loc = smpPlayer.getBukkitPlayer().getLocation();

        List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, 6).getHitMobs();

        for (LivingEntity mob : mobs) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4));
        }

        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 50,2,1,2);

        setCooldownTick(160);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
