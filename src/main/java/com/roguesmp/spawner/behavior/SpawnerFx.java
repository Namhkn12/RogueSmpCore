package com.roguesmp.spawner.behavior;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public final class SpawnerFx {

    private SpawnerFx() {}

    public static Location center(Location block) {
        return block.clone().add(0.5, 0.5, 0.5);
    }

    public static <T> void ring(Location center, double radius, int points, double phase, Particle particle, T data) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < points; i++) {
            double angle = phase + Math.PI * 2 * i / points;
            Location point = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0, data, true);
        }
    }

    public static <T> void column(Location center, double height, double step, Particle particle, T data) {
        World world = center.getWorld();
        if (world == null) return;
        for (double y = 0; y <= height; y += step) {
            world.spawnParticle(particle, center.clone().add(0, y, 0), 1, 0.05, 0, 0.05, 0, data, true);
        }
    }

    public static <T> void helix(Location center, double height, double radius, double phase, Particle particle, T data) {
        World world = center.getWorld();
        if (world == null) return;
        for (double y = 0; y <= height; y += 0.25) {
            double angle = phase + y * 1.5;
            for (int strand = 0; strand < 2; strand++) {
                double a = angle + Math.PI * strand;
                Location point = center.clone().add(Math.cos(a) * radius, y, Math.sin(a) * radius);
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0, data, true);
            }
        }
    }

    public static void sound(Location location, Sound sound, float volume, float pitch) {
        World world = location.getWorld();
        if (world != null) world.playSound(location, sound, volume, pitch);
    }
}
