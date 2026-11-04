package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ShadowLeapStep extends ComboStep {

    private static final double BEHIND_DISTANCE = 2.2;
    private static final int LOCK_TICKS = 8;
    private static final int STRIKE_DELAY = 8;
    private static final int RECOVER_AFTER = 10;
    private static final double STRIKE_RANGE = 3.5;
    private static final double STRIKE_HALF_ANGLE = Math.toRadians(70);

    private final double multiplier;
    private final String id;
    private @Nullable Player victim;
    private @Nullable Location destination;
    private boolean struck;

    public ShadowLeapStep(String title, int windup, double multiplier, String id) {
        super(title, StepKind.MELEE, windup);
        this.multiplier = multiplier;
        this.id = id;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        victim = ctx.aimTarget();
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 1.5f);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        if (victim == null || !victim.isValid()) return;
        if (elapsed < windup() - LOCK_TICKS || destination == null) destination = behind(ctx, victim);
        Fx.ring(destination, 1.0, Fx.ARCANE);
        destination.getWorld().spawnParticle(Particle.REVERSE_PORTAL, destination.clone().add(0, 1, 0), 8, 0.2, 0.6, 0.2, 0.02);
        ctx.owner().getWorld().spawnParticle(Particle.PORTAL, ctx.owner().getLocation().add(0, 1, 0), 10, 0.3, 0.6, 0.3, 0.5);
    }

    private Location behind(KingContext ctx, Player player) {
        Vector back = player.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-4) back = new Vector(1, 0, 0);
        Location spot = player.getLocation().subtract(back.normalize().multiply(BEHIND_DISTANCE));
        if (!spot.clone().add(0, 1, 0).getBlock().isPassable()) spot = player.getLocation();
        return ctx.onFloor(spot);
    }

    @Override
    public void onExecute(KingContext ctx) {
        if (destination == null) return;
        Location origin = ctx.owner().getLocation();
        origin.getWorld().spawnParticle(Particle.LARGE_SMOKE, origin.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.03);
        ctx.moveTo(destination);
        if (victim != null) ctx.face(victim.getLocation());
        destination.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        if (!struck && elapsed >= STRIKE_DELAY) {
            struck = true;
            strike(ctx);
        }
        return elapsed >= STRIKE_DELAY + RECOVER_AFTER;
    }

    private void strike(KingContext ctx) {
        Location origin = ctx.owner().getLocation();
        Vector facing = origin.getDirection().setY(0);
        ctx.owner().swingOffHand();
        origin.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.6f);
        origin.getWorld().spawnParticle(Particle.SWEEP_ATTACK, origin.clone().add(facing.clone().multiply(1.5)).add(0, 1, 0), 2, 0.3, 0.2, 0.3, 0);
        for (Player player : ctx.players()) {
            Vector offset = player.getLocation().toVector().subtract(origin.toVector()).setY(0);
            if (offset.lengthSquared() > STRIKE_RANGE * STRIKE_RANGE) continue;
            if (offset.lengthSquared() > 1 && facing.lengthSquared() > 0 && offset.angle(facing) > STRIKE_HALF_ANGLE) continue;
            ctx.damage(player, multiplier, DamageType.MELEE, id);
            ctx.push(player, origin, 0.9, 0.4);
        }
    }
}
