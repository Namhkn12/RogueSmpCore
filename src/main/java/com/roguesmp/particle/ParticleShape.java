package com.roguesmp.particle;

import org.bukkit.util.Vector;

import java.util.List;

public interface ParticleShape {
    /**
     * Returns a list of relative offsets for a specific tick.
     */
    List<Vector> getOffsets(int tick);
}
