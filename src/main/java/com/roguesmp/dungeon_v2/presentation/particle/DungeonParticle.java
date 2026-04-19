package com.roguesmp.dungeon_v2.presentation.particle;

import org.bukkit.Particle;

public class DungeonParticle {

    private final Particle particle;
    private final int count;
    private final double offsetX, offsetY, offsetZ;
    private final double extra;

    private DungeonParticle(Particle particle, int count,
                            double offsetX, double offsetY, double offsetZ,
                            double extra) {
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;
    }

    public static DungeonParticle of(Particle particle, int count,
                                     double offsetX, double offsetY, double offsetZ) {
        return new DungeonParticle(particle, count, offsetX, offsetY, offsetZ, 0);
    }

    public static DungeonParticle of(Particle particle, int count,
                                     double offsetX, double offsetY, double offsetZ,
                                     double extra) {
        return new DungeonParticle(particle, count, offsetX, offsetY, offsetZ, extra);
    }

    // getters...
    public Particle getParticle() { return particle; }
    public int getCount() { return count; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public double getExtra() { return extra; }
}