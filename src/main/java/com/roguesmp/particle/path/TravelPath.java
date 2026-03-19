package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

// Where the center of the effect is going (Trajectory)
public interface TravelPath {
    Location getLocation(int tick);
    Vector getDirection();
    boolean isFinished(int tick, Location current);
}
