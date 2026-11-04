package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AxeWhirlStep extends ComboStep {

    private static final int HIT_INTERVAL = 10;
    private static final double DRIFT_SPEED = 0.12;
    private static final float SPIN_DEGREES = 60f;

    private final int duration;
    private final double radius;
    private final double multiplier;
    private final String id;

    public AxeWhirlStep(String title, int windup, int duration, double radius, double multiplier, String id) {
        super(title, StepKind.MELEE, windup);
        this.duration = duration;
        this.radius = radius;
        this.multiplier = multiplier;
        this.id = id;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.5f, 0.6f);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        Fx.ring(ctx.owner().getLocation(), radius * elapsed / Math.max(1, windup()), Fx.WARNING);
        Fx.ring(ctx.owner().getLocation(), radius, Particle.SMOKE);
    }

    @Override
    public void onExecute(KingContext ctx) {
        spin(ctx, 0);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        Player target = ctx.aimTarget();
        if (target != null) {
            Vector drift = SpellTargets.horizontalDirection(ctx.owner().getLocation(), target.getLocation())
                    .multiply(DRIFT_SPEED * SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT);
            Location next = ctx.owner().getLocation().add(drift);
            if (next.clone().add(0, 1, 0).getBlock().isPassable()) ctx.moveTo(next);
        }
        spin(ctx, elapsed);
        return elapsed >= duration;
    }

    private void spin(KingContext ctx, int elapsed) {
        Location center = ctx.owner().getLocation();
        ctx.owner().setRotation(center.getYaw() + SPIN_DEGREES, 0);
        ctx.owner().swingMainHand();
        for (int i = 0; i < 4; i++) {
            double angle = elapsed * 0.6 + Math.PI / 2 * i;
            center.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                    center.clone().add(Math.cos(angle) * radius * 0.7, 1, Math.sin(angle) * radius * 0.7), 1, 0, 0, 0, 0);
        }
        Fx.ring(center, radius, Particle.CRIT);
        if (elapsed % HIT_INTERVAL != 0) return;

        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
        for (Player player : ctx.players()) {
            if (player.getLocation().distanceSquared(center) > radius * radius) continue;
            if (Math.abs(player.getY() - center.getY()) > 2) continue;
            ctx.damage(player, multiplier, DamageType.MELEE, id);
            ctx.push(player, center, 0.5, 0.25);
        }
    }
}
