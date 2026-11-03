package com.roguesmp.entity.boss.goblinking;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public final class Fx {

    public static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(200, 15, 25), 1.3f);
    public static final Particle.DustOptions ARCANE = new Particle.DustOptions(Color.fromRGB(170, 60, 255), 1.2f);
    public static final Particle.DustOptions EARTH = new Particle.DustOptions(Color.fromRGB(120, 255, 90), 1.2f);
    public static final Particle.DustOptions MAGMA = new Particle.DustOptions(Color.fromRGB(255, 110, 0), 1.3f);
    public static final Particle.DustOptions STORM = new Particle.DustOptions(Color.fromRGB(120, 200, 255), 1.2f);
    public static final Particle.DustOptions WARNING = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 1.0f);
    public static final Particle.DustOptions ROYAL = new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.6f);
    public static final Particle.DustOptions FEL = new Particle.DustOptions(Color.fromRGB(60, 255, 60), 1.5f);

    private static final double SPACING = 0.5;

    private Fx() {
    }

    public static void ring(Location center, double radius, Particle.DustOptions dust) {
        int points = Math.max(8, (int) (Math.PI * 2 * radius / SPACING));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            center.getWorld().spawnParticle(Particle.DUST, center.getX() + Math.cos(angle) * radius, center.getY() + 0.1,
                    center.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0, dust);
        }
    }

    public static void ring(Location center, double radius, Particle particle) {
        int points = Math.max(8, (int) (Math.PI * 2 * radius / SPACING));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            center.getWorld().spawnParticle(particle, center.getX() + Math.cos(angle) * radius, center.getY() + 0.1,
                    center.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void line(Location from, Vector direction, double length, Particle.DustOptions dust) {
        Vector step = direction.clone().normalize().multiply(SPACING);
        Location point = from.clone().add(0, 0.1, 0);
        for (double travelled = 0; travelled < length; travelled += SPACING) {
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
            point.add(step);
        }
    }

    public static void cone(Location origin, Vector facing, double range, double halfAngle, Particle.DustOptions dust) {
        double base = Math.atan2(facing.getZ(), facing.getX());
        for (double r = 1; r <= range; r += 1) {
            int points = Math.max(3, (int) (2 * halfAngle * r / SPACING));
            for (int i = 0; i <= points; i++) {
                double angle = base - halfAngle + 2 * halfAngle * i / points;
                origin.getWorld().spawnParticle(Particle.DUST, origin.getX() + Math.cos(angle) * r, origin.getY() + 0.1,
                        origin.getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0, dust);
            }
        }
    }
}
