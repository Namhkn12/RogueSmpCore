package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CleaveStep extends ComboStep {

    private static final int LOCK_TICKS = 4;
    private static final double LUNGE = 0.8;

    private final Aim aim;
    private final double range;
    private final double halfAngle;
    private final double multiplier;
    private final int hits;
    private final int hitGap;
    private final double finalShockwave;
    private final String id;
    private Vector facing = new Vector(1, 0, 0);
    private int swings;

    public CleaveStep(String title, int windup, Aim aim, double range, double halfAngleDegrees, double multiplier, String id) {
        this(title, windup, aim, range, halfAngleDegrees, multiplier, 1, 0, 0, id);
    }

    public CleaveStep(String title, int windup, Aim aim, double range, double halfAngleDegrees, double multiplier,
                      int hits, int hitGap, double finalShockwave, String id) {
        super(title, StepKind.MELEE, windup);
        this.aim = aim;
        this.range = range;
        this.halfAngle = Math.toRadians(halfAngleDegrees);
        this.multiplier = multiplier;
        this.hits = hits;
        this.hitGap = hitGap;
        this.finalShockwave = finalShockwave;
        this.id = id;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, 1.5f, 0.6f);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        if (elapsed < windup() - LOCK_TICKS) aimAt(ctx);
        Fx.cone(ctx.owner().getLocation(), facing, range * elapsed / Math.max(1, windup()), halfAngle, Fx.WARNING);
    }

    private void aimAt(KingContext ctx) {
        Location target = aim.resolve(ctx);
        if (target == null) return;
        Vector direction = SpellTargets.horizontalDirection(ctx.owner().getLocation(), target);
        if (direction.lengthSquared() < 1.0E-4) return;
        facing = direction;
        ctx.face(ctx.owner().getLocation().add(facing));
    }

    @Override
    public void onExecute(KingContext ctx) {
        swing(ctx);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        if (swings >= hits) return true;
        if (elapsed < swings * hitGap) {
            if (elapsed >= swings * hitGap - LOCK_TICKS * 2) Fx.cone(ctx.owner().getLocation(), facing, range, halfAngle, Fx.WARNING);
            return false;
        }
        aimAt(ctx);
        Location next = ctx.owner().getLocation().add(facing.clone().multiply(LUNGE));
        if (next.clone().add(0, 1, 0).getBlock().isPassable()) ctx.moveTo(next);
        swing(ctx);
        return swings >= hits;
    }

    private void swing(KingContext ctx) {
        swings++;
        Location origin = ctx.owner().getLocation();
        ctx.owner().swingMainHand();
        origin.getWorld().playSound(origin, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.6f);
        origin.getWorld().playSound(origin, Sound.ITEM_AXE_SCRAPE, 1.2f, 0.5f);
        for (double r = 1; r <= range; r += 1.2) {
            origin.getWorld().spawnParticle(Particle.SWEEP_ATTACK, origin.clone().add(facing.clone().multiply(r)).add(0, 1, 0), 1, 0, 0, 0, 0);
        }

        for (Player player : ctx.players()) {
            Vector offset = player.getLocation().toVector().subtract(origin.toVector()).setY(0);
            if (offset.lengthSquared() > range * range || Math.abs(player.getY() - origin.getY()) > 2.5) continue;
            if (offset.lengthSquared() > 1 && offset.angle(facing) > halfAngle) continue;
            ctx.damage(player, multiplier, DamageType.MELEE, id);
            ctx.push(player, origin, 0.6, 0.35);
        }

        if (swings == hits && finalShockwave > 0) {
            ctx.addHazard(new Shockwave(ctx, origin, finalShockwave, multiplier * 0.6, Fx.WARNING, id));
        }
    }
}
