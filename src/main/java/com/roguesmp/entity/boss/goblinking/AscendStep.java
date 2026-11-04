package com.roguesmp.entity.boss.goblinking;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AscendStep extends ComboStep {

    private final double height;
    private final int riseTicks;
    private final int holdTicks;
    private final Volley volley;
    private Location start;
    private int volleyStart = -1;

    public AscendStep(String title, StepKind kind, int windup, double height, int riseTicks, int holdTicks, Volley volley) {
        super(title, kind, windup);
        this.height = height;
        this.riseTicks = riseTicks;
        this.holdTicks = holdTicks;
        this.volley = volley;
    }

    public static AscendStep grounded(String title, int windup, int duration, Volley volley) {
        return new AscendStep(title, StepKind.MAGIC, windup, 0, 0, duration, volley);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        if (height <= 0) {
            Player target = ctx.aimTarget();
            if (target != null) ctx.face(target.getLocation());
            ctx.owner().getWorld().spawnParticle(Particle.FLAME, ctx.owner().getLocation().add(0, 1.2, 0), 5, 0.4, 0.4, 0.4, 0.01);
            return;
        }
        ctx.owner().getWorld().spawnParticle(Particle.CLOUD, ctx.owner().getLocation(), 4, 0.4, 0, 0.4, 0.03);
    }

    @Override
    public void onExecute(KingContext ctx) {
        start = ctx.owner().getLocation();
        ctx.owner().swingMainHand();
        if (height <= 0) return;
        ctx.hover(true);
        ctx.owner().getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);
        ctx.owner().getWorld().spawnParticle(Particle.EXPLOSION, start, 2, 0.5, 0, 0.5, 0);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        if (height > 0 && elapsed <= riseTicks) {
            double t = (double) elapsed / Math.max(1, riseTicks);
            double eased = 1 - (1 - t) * (1 - t);
            ctx.moveTo(start.clone().add(0, height * eased, 0));
            ctx.owner().getWorld().spawnParticle(Particle.CLOUD, ctx.owner().getLocation(), 2, 0.2, 0, 0.2, 0.01);
            return false;
        }
        if (volleyStart < 0) volleyStart = elapsed;
        volley.tick(ctx, elapsed - volleyStart);
        return elapsed >= riseTicks + holdTicks;
    }

    @Override
    public void onCancel(KingContext ctx) {
        ctx.hover(false);
    }
}
