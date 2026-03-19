package com.roguesmp.player.ability.impl;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.particle.ComposableEffect;
import com.roguesmp.particle.path.HomingPath;
import com.roguesmp.particle.path.TravelPath;
import com.roguesmp.particle.shape.ParticleShape;
import com.roguesmp.particle.shape.SpinningShape;
import com.roguesmp.particle.shape.StarShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class ArcaneSpiral extends Ability {

    public static final String ID = "arcane_spiral";

    public static final AbilityInfo<ArcaneSpiral> INFO =
            new AbilityInfo<>(
                    ID,
                    (player, level) -> List.of(
                            Component.text("Triệu hồi xoáy năng lượng quanh bản thân").decoration(TextDecoration.ITALIC,false),
                            Component.text("Gây sát thương cho kẻ địch gần đó").decoration(TextDecoration.ITALIC, false)
                    ),
                    Component.text("Arcane Spiral", NamedTextColor.LIGHT_PURPLE),
                    Material.AMETHYST_SHARD,
                    ArcaneSpiral::new,
                    AbilityTrigger.RIGHT_CLICK
            );

    public ArcaneSpiral(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
    }

    @Override
    public void cast() {
        Player player = smpPlayer.getBukkitPlayer();
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.FLAME).count(0);
        // Visuals: A spinning star
        ParticleShape star = new SpinningShape(new StarShape(2, 1), 0.8);

        // TravelPath: Homing missile with specific speed, turn rate, and lifetime
        TravelPath physics = new HomingPath(
                player.getEyeLocation(),
                0.7,
                0.12,
                100,
                () -> findNearestEnemy(player, 10)
        );

        new ComposableEffect(star, physics, particleBuilder, (loc, dir, t, finished) -> {
            if (finished) {
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                return true;
            }

            LivingEntity hit = (LivingEntity) loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)
                    .stream().filter(e -> e instanceof Monster).findFirst().orElse(null);

            if (hit != null) {
                hit.damage(12.0, player);
                return true;
            }
            return false;
        }).runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

//        setCooldownTick(20 * (8 - Math.min(level, 5)));
    }

    public static LivingEntity findNearestEnemy(Player player, double range) {
        return player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> !entity.equals(player) && !entity.isDead())
                .min(Comparator.comparingDouble(entity ->
                        entity.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
