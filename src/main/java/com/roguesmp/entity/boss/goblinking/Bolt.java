package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class Bolt implements Hazard {

    private static final double HIT_RADIUS = 0.9;

    private final KingContext ctx;
    private final Location position;
    private final Vector velocity;
    private final @Nullable Player homing;
    private final double turnRate;
    private final double gravity;
    private final double multiplier;
    private final double blastRadius;
    private final Particle trail;
    private final DamageType type;
    private final String id;
    private int life;

    public Bolt(KingContext ctx, Location from, Vector velocity, @Nullable Player homing, double turnRate, double gravity,
                int life, double multiplier, double blastRadius, Particle trail, DamageType type, String id) {
        this.ctx = ctx;
        this.position = from.clone();
        this.velocity = velocity.clone();
        this.homing = homing;
        this.turnRate = turnRate;
        this.gravity = gravity;
        this.life = life;
        this.multiplier = multiplier;
        this.blastRadius = blastRadius;
        this.trail = trail;
        this.type = type;
        this.id = id;
    }

    @Override
    public boolean tick(int interval) {
        for (int i = 0; i < interval; i++) {
            if (step()) return true;
        }
        return false;
    }

    private boolean step() {
        if (--life <= 0) return true;
        if (homing != null && homing.isValid() && !homing.isDead()) {
            Vector desired = homing.getLocation().add(0, 1, 0).toVector().subtract(position.toVector());
            if (desired.lengthSquared() > 1.0E-4) {
                double speed = velocity.length();
                velocity.add(desired.normalize().multiply(speed * turnRate)).normalize().multiply(speed);
            }
        }
        velocity.setY(velocity.getY() - gravity);
        position.add(velocity);
        position.getWorld().spawnParticle(trail, position, 2, 0.05, 0.05, 0.05, 0);

        if (!position.getBlock().isPassable()) {
            burst();
            return true;
        }
        for (Player player : ctx.players()) {
            if (player.getBoundingBox().expand(HIT_RADIUS * 0.5).contains(position.toVector())) {
                burst();
                return true;
            }
        }
        return false;
    }

    private void burst() {
        position.getWorld().spawnParticle(Particle.EXPLOSION, position, 1, 0, 0, 0, 0);
        position.getWorld().playSound(position, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        double radius = Math.max(HIT_RADIUS, blastRadius);
        for (Player player : ctx.players()) {
            if (player.getLocation().add(0, 1, 0).distanceSquared(position) > radius * radius) continue;
            ctx.damage(player, multiplier, type, id);
            if (type == DamageType.FIRE) player.setFireTicks(Math.max(player.getFireTicks(), 60));
        }
    }
}
