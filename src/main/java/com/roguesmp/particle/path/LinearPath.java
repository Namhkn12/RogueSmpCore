package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class LinearPath implements ParticlePath {

    private final Location start;
    private final Vector velocity;

    public LinearPath(Location start, Vector velocity) {
        this.start = start.clone();
        this.velocity = velocity.clone();
    }

    @Override
    public Location getPosition(int tick) {
        return start.clone().add(velocity.clone().multiply(tick));
    }

    @Override
    public Vector getDirection(int tick) {
        return velocity.clone().normalize();
    }
}
