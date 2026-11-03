package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChargeStep extends ComboStep {

    private static final double HIT_RADIUS = 1.6;
    private static final double SUB_STEP = 0.5;
    private static final int LOCK_TICKS = 6;
    private static final int STAGGER_TICKS = 20;

    private final Aim aim;
    private final double speed;
    private final double maxDistance;
    private final double multiplier;
    private final double knockback;
    private final String id;
    private final Set<UUID> hit = new HashSet<>();
    private Vector direction = new Vector(1, 0, 0);
    private double travelled;
    private int staggerUntil = -1;

    public ChargeStep(String title, int windup, Aim aim, double speed, double maxDistance, double multiplier, double knockback, String id) {
        super(title, StepKind.MELEE, windup);
        this.aim = aim;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.multiplier = multiplier;
        this.knockback = knockback;
        this.id = id;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.3f);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        if (elapsed < windup() - LOCK_TICKS) aimAt(ctx);
        Fx.line(ctx.owner().getLocation(), direction, maxDistance, Fx.WARNING);
        ctx.owner().getWorld().spawnParticle(Particle.CLOUD, ctx.owner().getLocation(), 3, 0.3, 0, 0.3, 0.02);
    }

    private void aimAt(KingContext ctx) {
        Location target = aim.resolve(ctx);
        if (target == null) return;
        direction = SpellTargets.horizontalDirection(ctx.owner().getLocation(), target);
        if (direction.lengthSquared() < 1.0E-4) direction = new Vector(1, 0, 0);
        ctx.face(ctx.owner().getLocation().add(direction));
    }

    @Override
    public void onExecute(KingContext ctx) {
        ctx.owner().swingMainHand();
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 1.5f, 0.6f);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        if (staggerUntil >= 0) return elapsed >= staggerUntil;

        double budget = speed * SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT;
        while (budget > 0 && travelled < maxDistance) {
            double move = Math.min(SUB_STEP, budget);
            Location next = ctx.owner().getLocation().add(direction.clone().multiply(move));
            double floor = ctx.floorY(next.clone().add(0, 1, 0));
            if (Math.abs(floor - next.getY()) > 1.1 || !next.clone().add(0, 1, 0).getBlock().isPassable()) {
                bump(ctx, elapsed);
                return false;
            }
            next.setY(floor);
            ctx.moveTo(next);
            travelled += move;
            budget -= move;
            hitPlayers(ctx);
        }
        ctx.owner().getWorld().spawnParticle(Particle.CLOUD, ctx.owner().getLocation(), 4, 0.3, 0.1, 0.3, 0.02);
        return travelled >= maxDistance;
    }

    private void hitPlayers(KingContext ctx) {
        Location at = ctx.owner().getLocation();
        for (Player player : ctx.players()) {
            if (hit.contains(player.getUniqueId())) continue;
            if (player.getLocation().distanceSquared(at) > HIT_RADIUS * HIT_RADIUS) continue;
            hit.add(player.getUniqueId());
            ctx.damage(player, multiplier, DamageType.MELEE, id);
            ctx.push(player, at, knockback, 0.5);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
        }
    }

    private void bump(KingContext ctx, int elapsed) {
        staggerUntil = elapsed + STAGGER_TICKS;
        Location at = ctx.owner().getLocation().add(direction).add(0, 1, 0);
        at.getWorld().spawnParticle(Particle.BLOCK, at, 30, 0.4, 0.4, 0.4, 0, at.getBlock().getBlockData());
        at.getWorld().spawnParticle(Particle.CRIT, ctx.owner().getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.1);
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.6f);
    }
}
