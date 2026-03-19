package com.roguesmp.particle.shape;

import org.bukkit.util.Vector;

import java.util.List;

public interface ParticleShape {
    List<Vector> getPoints(int tick);
}
