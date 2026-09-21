package com.roguesmp.fx.render;

import org.bukkit.World;
import org.joml.Vector3f;

/**
 * One shape point being rendered this tick, handed to a caller-supplied callback. {@link #position}
 * is the mutable JOML vector the renderer will actually use afterward — a callback that wants to
 * nudge a point (a bob, a jitter) mutates it in place rather than returning a new one.
 */
public record FxPoint(World world, Vector3f position, int index, int tick) {
}
