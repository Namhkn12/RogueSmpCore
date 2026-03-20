package com.roguesmp.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.particle.path.TravelPath;
import com.roguesmp.particle.shape.ParticleShape;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ComposableEffect extends BukkitRunnable {
    private final ParticleShape shape;
    private final TravelPath path;
    private final ParticleBuilder particle;
    private final EffectController controller;
    private int tick = 0;

    public ComposableEffect(ParticleShape s, TravelPath p, ParticleBuilder part, EffectController c) {
        this.shape = s;
        this.path = p;
        this.particle = part;
        this.controller = c;
    }

    @Override
    public void run() {
        Location loc = path.getLocation(tick);
        Vector dir = path.getDirection();
        boolean finished = path.isFinished(tick, loc);

        // 1. Logic Hook
        if (controller.update(loc, dir, tick, finished)) {
            this.cancel();
            return;
        }

        // 2. Calculate the rotation "Basis"
        // This defines the "Right" and "Up" vectors relative to travel direction
        Vector forward = dir.clone().normalize();
        Vector tempUp = (Math.abs(forward.getY()) > 0.9) ? new Vector(1, 0, 0) : new Vector(0, 1, 0);

        Vector right = forward.clone().crossProduct(tempUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        // 3. Render Loop
        for (Vector v : shape.getPoints(tick)) {
            // Transform the point using the pre-calculated basis
            Vector rotated = right.clone().multiply(v.getX())
                    .add(up.clone().multiply(v.getY()))
                    .add(forward.clone().multiply(v.getZ()));

            // Reuse the builder's location to avoid heavy cloning
            particle.location(loc.clone().add(rotated)).spawn();
        }
        tick++;
    }
}
