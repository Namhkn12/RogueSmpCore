package com.roguesmp.fx.render;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.fx.FxTransform;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

/** Spawns one or more particles at every point of a shape, each tick. Stateless — nothing to clean up. */
public final class ParticleRenderer implements FxRenderer {

    private final List<ParticleBuilder> builders;

    public ParticleRenderer(ParticleBuilder builder) {
        this(List.of(builder));
    }

    public ParticleRenderer(List<ParticleBuilder> builders) {
        this.builders = builders;
    }

    @Override
    public void render(World world, FxTransform transform, List<Vector> localPoints, int tick, Collection<Player> viewers) {
        // One cheap chunk check per part per tick (not per point) — good enough to skip the common
        // case of the whole area being unloaded without forcing a load or paying a per-point lookup.
        Vector3f rootPos = transform.position();
        if (!world.isChunkLoaded((int) rootPos.x() >> 4, (int) rootPos.z() >> 4)) return;

        for (Vector local : localPoints) {
            Vector3f worldPos = transform.apply(local);
            for (ParticleBuilder builder : builders) {
                builder.location(world, worldPos.x(), worldPos.y(), worldPos.z());
                if (viewers != null) {
                    builder.receivers(viewers);
                }
                builder.spawn();
            }
        }
    }

    @Override
    public void remove() {
        // No persistent state.
    }
}
