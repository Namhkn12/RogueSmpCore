package com.roguesmp.player.ability.impl;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.particle.ParticleTask;
import com.roguesmp.particle.animation.ParticleAnimation;
import com.roguesmp.particle.animation.SpinAnimation;
import com.roguesmp.particle.path.LinearPath;
import com.roguesmp.particle.path.ParticlePath;
import com.roguesmp.particle.shape.ParticleShape;
import com.roguesmp.particle.shape.SpiralShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        ParticleBuilder builder = new ParticleBuilder(Particle.FLAME).count(0);
        ParticleShape spiral = new SpiralShape(1.5, 3, 4, 10);

        ParticlePath path = new LinearPath(
                player.getEyeLocation(),
                player.getLocation().getDirection().multiply(0.3)
        );

        ParticleAnimation spin = new SpinAnimation(0.25);

        new ParticleTask(builder, spiral, path, spin, 120).start();
        // cooldown
//        setCooldownTick(20 * (8 - Math.min(level, 5)));
    }


    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
