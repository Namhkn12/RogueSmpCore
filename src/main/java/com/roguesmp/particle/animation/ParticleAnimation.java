package com.roguesmp.particle.animation;

import org.bukkit.util.Vector;

public interface ParticleAnimation {

    void prepare(int tick, Vector direction);

    Vector apply(Vector point);

}
