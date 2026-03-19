package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.function.Supplier;

public class HomingPath implements TravelPath {
    private final Location current;
    private final Supplier<LivingEntity> targetSupplier;
    private final double speed;
    private final double turnRate;
    private final int maxLifetime;
    private Vector currentDir;

    public HomingPath(Location start, double speed, double turnRate, int maxLifetime, Supplier<LivingEntity> targetSupplier) {
        this.current = start.clone();
        this.currentDir = start.getDirection().normalize();
        this.speed = speed;
        this.turnRate = turnRate;
        this.maxLifetime = maxLifetime;
        this.targetSupplier = targetSupplier;
    }

    @Override
    public Location getLocation(int tick) {
        LivingEntity target = targetSupplier.get();
        if (target != null && !target.isDead()) {
            Vector toTarget = target.getEyeLocation().toVector().subtract(current.toVector()).normalize();
            // Interpolate current direction toward target
            currentDir.add(toTarget.multiply(turnRate)).normalize();
        }
        return current.add(currentDir.clone().multiply(speed));
    }

    @Override public Vector getDirection() { return currentDir; }
    @Override public boolean isFinished(int tick, Location loc) {
        return tick >= maxLifetime || loc.getBlock().getType().isSolid();
    }
}
