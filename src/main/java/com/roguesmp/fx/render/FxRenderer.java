package com.roguesmp.fx.render;

import com.roguesmp.fx.FxTransform;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

/** Draws a shape's points, already-transformed to world space via the given {@link FxTransform}, every tick. */
public interface FxRenderer {

    /** @param viewers who should see this render; {@code null} means everyone nearby (the default). */
    void render(World world, FxTransform transform, List<Vector> localPoints, int tick, Collection<Player> viewers);

    /** Cleans up any entities/state this renderer owns. Called when the owning part/effect stops. */
    void remove();
}
