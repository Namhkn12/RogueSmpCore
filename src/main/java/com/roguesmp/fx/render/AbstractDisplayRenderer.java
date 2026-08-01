package com.roguesmp.fx.render;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.fx.FxTransform;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Keeps one real display entity per shape point, repositioning/reorienting them as the shape's
 * transform changes instead of respawning. Entity count is grown/shrunk lazily to match the shape.
 */
abstract class AbstractDisplayRenderer<T extends Display> implements FxRenderer {

    private final Class<T> type;
    private final Vector3f displaySize;
    private final int interpolationTicks;
    private final List<T> entities = new ArrayList<>();

    protected AbstractDisplayRenderer(Class<T> type, Vector3f displaySize, int interpolationTicks) {
        this.type = type;
        this.displaySize = displaySize;
        this.interpolationTicks = interpolationTicks;
    }

    /** Applies renderer-specific setup (block/item, billboard, etc.) to a freshly spawned entity. */
    protected abstract void configure(T display);

    @Override
    public void render(World world, FxTransform transform, List<Vector> localPoints, int tick, Collection<Player> viewers) {
        // Same one-check-per-tick trade-off as ParticleRenderer. Skipping here also matters more:
        // spawning/teleporting real entities into an unloaded chunk is the thing we most want to avoid.
        Vector3f rootPos = transform.position();
        if (!world.isChunkLoaded((int) rootPos.x() >> 4, (int) rootPos.z() >> 4)) return;

        // Drop any entity the server already removed out from under us (e.g. a non-persistent
        // entity swept on chunk unload) before counting capacity, so it gets respawned instead of
        // teleport()/setTransformation() being called on a dead reference below.
        entities.removeIf(display -> !display.isValid());
        ensureCapacity(world, localPoints.size(), viewers);

        Quaternionf rotation = transform.rotation();
        Vector3f halfSize = new Vector3f(displaySize).mul(-0.5f);

        for (int i = 0; i < localPoints.size(); i++) {
            Vector3f worldPos = transform.apply(localPoints.get(i));
            T display = entities.get(i);

            display.teleport(new Location(world, worldPos.x(), worldPos.y(), worldPos.z()));
            display.setTransformation(new Transformation(halfSize, rotation, displaySize, new Quaternionf()));
        }
    }

    private void ensureCapacity(World world, int needed, Collection<Player> viewers) {
        while (entities.size() < needed) {
            T display = world.spawn(world.getSpawnLocation(), type, d -> {
                configure(d);
                d.setPersistent(false);
                d.setInterpolationDuration(interpolationTicks);
                d.setInterpolationDelay(0);

                // Scope visibility once, at spawn — viewers isn't expected to change over an
                // effect's lifetime, so there's no need to redo this on every render() call.
                if (viewers != null) {
                    d.setVisibleByDefault(false);
                    for (Player viewer : viewers) {
                        viewer.showEntity(RogueSmpCore.getInstance(), d);
                    }
                }
            });
            entities.add(display);
        }
        while (entities.size() > needed) {
            entities.removeLast().remove();
        }
    }

    @Override
    public void remove() {
        entities.forEach(Entity::remove);
        entities.clear();
    }
}
