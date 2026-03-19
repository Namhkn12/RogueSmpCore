package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface ParticlePath {

    Location getPosition(int tick);

    Vector getDirection(int tick);
}