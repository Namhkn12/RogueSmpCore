package com.roguesmp.particle;

import org.bukkit.Location;
import org.bukkit.util.Vector;

@FunctionalInterface
public interface EffectController {
    boolean update(Location location, Vector direction, int tick, boolean pathFinished);
}