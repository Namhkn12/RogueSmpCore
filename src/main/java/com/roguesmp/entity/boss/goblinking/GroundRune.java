package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class GroundRune implements Hazard {

    public enum Style {
        ROCK(Fx.EARTH, DamageType.MELEE, 1.3, 2.2),
        MAGMA(Fx.MAGMA, DamageType.FIRE, 1.2, 2.2),
        LIGHTNING(Fx.STORM, DamageType.MAGIC, 1.4, 2.5),
        LAVA(Fx.MAGMA, DamageType.FIRE, 1.5, 2.8);

        private final Particle.DustOptions dust;
        private final DamageType type;
        private final double multiplier;
        private final double radius;

        Style(Particle.DustOptions dust, DamageType type, double multiplier, double radius) {
            this.dust = dust;
            this.type = type;
            this.multiplier = multiplier;
            this.radius = radius;
        }
    }

    private static final BlockData SPIKE = Material.POINTED_DRIPSTONE.createBlockData();
    private static final int SPIKE_LIFETIME = 30;
    private static final int LINGER_INTERVAL = 10;
    private static final double LINGER_MULTIPLIER = 0.15;

    private final KingContext ctx;
    private final Location center;
    private final Style style;
    private final String id;
    private int fuse;
    private int age;
    private boolean detonated;

    public GroundRune(KingContext ctx, Location center, Style style, int fuse, String id) {
        this.ctx = ctx;
        this.center = center.clone();
        this.style = style;
        this.fuse = fuse;
        this.id = id;
    }

    public Location center() {
        return center.clone();
    }

    @Override
    public boolean tick(int interval) {
        if (detonated) return true;
        age += interval;
        fuse -= interval;
        if (fuse <= 0) {
            detonate();
            return true;
        }

        double pulse = style.radius * (0.85 + 0.15 * Math.sin(age * 0.4));
        Fx.ring(center, pulse, style.dust);
        if (age % 6 == 0) Fx.ring(center, style.radius * 0.45, style.dust);
        if (style == Style.MAGMA) {
            center.getWorld().spawnParticle(Particle.LAVA, center, 1, style.radius * 0.4, 0, style.radius * 0.4, 0);
            if (age % LINGER_INTERVAL == 0) linger();
        }
        return false;
    }

    private void linger() {
        for (Player player : ctx.players()) {
            if (SpellTargets.horizontalDistance(player.getLocation(), center) > style.radius * 0.8) continue;
            if (Math.abs(player.getY() - center.getY()) > 1.5) continue;
            ctx.damage(player, LINGER_MULTIPLIER, DamageType.FIRE, id);
            player.setFireTicks(Math.max(player.getFireTicks(), 20));
        }
    }

    public void detonate() {
        if (detonated) return;
        detonated = true;

        switch (style) {
            case ROCK -> {
                center.getWorld().spawnParticle(Particle.BLOCK, center, 40, 0.8, 0.4, 0.8, 0, Material.STONE.createBlockData());
                center.getWorld().playSound(center, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1.5f, 0.6f);
                spawnSpike();
            }
            case MAGMA, LAVA -> {
                center.getWorld().spawnParticle(Particle.LAVA, center, 25, 0.6, 0.3, 0.6, 0);
                center.getWorld().spawnParticle(Particle.FLAME, center, 40, 0.3, 1.5, 0.3, 0.08);
                center.getWorld().playSound(center, Sound.BLOCK_LAVA_POP, 2f, 0.6f);
                center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
            }
            case LIGHTNING -> {
                center.getWorld().strikeLightningEffect(center);
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 40, 0.6, 1.2, 0.6, 0.1);
            }
        }

        for (Player player : ctx.players()) {
            if (SpellTargets.horizontalDistance(player.getLocation(), center) > style.radius) continue;
            if (player.getY() - center.getY() > 2.5 || player.getY() < center.getY() - 1.5) continue;
            ctx.damage(player, style.multiplier, style.type, id);
            player.setVelocity(player.getVelocity().setY(0.6));
            if (style.type == DamageType.FIRE) player.setFireTicks(Math.max(player.getFireTicks(), 60));
        }
    }

    private void spawnSpike() {
        BlockDisplay spike = center.getWorld().spawn(center, BlockDisplay.class, display -> {
            display.setBlock(SPIKE);
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.75f, 0, -0.75f), new AxisAngle4f(), new Vector3f(1.5f, 2.5f, 1.5f), new AxisAngle4f()));
        });
        spike.getScheduler().runDelayed(RogueSmpCore.getInstance(), task -> spike.remove(), null, SPIKE_LIFETIME);
    }
}
