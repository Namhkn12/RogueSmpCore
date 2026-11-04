package com.roguesmp.entity.boss.goblinking;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class PlungeStep extends ComboStep {

    private static final double MARKER_RADIUS = 2.5;
    private static final double AIRBORNE_THRESHOLD = 2.0;

    private final Aim aim;
    private final double apex;
    private final int airTicks;
    private final Impact impact;
    private Location start;
    private Location landing;
    private boolean fromAir;

    public PlungeStep(String title, int windup, Aim aim, double apex, int airTicks, Impact impact) {
        super(title, StepKind.AERIAL, windup);
        this.aim = aim;
        this.apex = apex;
        this.airTicks = airTicks;
        this.impact = impact;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_RAVAGER_STUNNED, 1.2f, 0.8f);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        Location target = aim.resolve(ctx);
        if (target == null) return;
        ctx.face(target);
        Fx.ring(ctx.onFloor(target), MARKER_RADIUS * elapsed / Math.max(1, windup()), Fx.WARNING);
    }

    @Override
    public void onExecute(KingContext ctx) {
        Location target = aim.resolve(ctx);
        start = ctx.owner().getLocation();
        landing = ctx.onFloor(target != null ? target : start);
        fromAir = start.getY() - ctx.floorY(start) > AIRBORNE_THRESHOLD;
        ctx.hover(true);
        ctx.face(landing);
        ctx.owner().getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.2f);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        double t = Math.min(1, (double) elapsed / Math.max(1, airTicks));
        Location position = start.clone().add(landing.toVector().subtract(start.toVector()).multiply(fromAir ? t * t : t));
        if (!fromAir) position.add(0, 4 * apex * t * (1 - t), 0);
        ctx.moveTo(position);
        Fx.ring(landing, MARKER_RADIUS, Fx.WARNING);
        ctx.owner().getWorld().spawnParticle(fromAir ? Particle.FLAME : Particle.CLOUD, position, 3, 0.2, 0.2, 0.2, 0.02);
        if (t < 1) return false;

        ctx.hover(false);
        ctx.owner().swingMainHand();
        impact.land(ctx, landing);
        return true;
    }

    @Override
    public void onCancel(KingContext ctx) {
        ctx.hover(false);
    }
}
