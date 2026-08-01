package com.roguesmp.fx;

import com.roguesmp.fx.motion.FxMotion;
import com.roguesmp.fx.render.FxRenderer;
import com.roguesmp.fx.shape.FxShape;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.IntPredicate;

/**
 * One visual element of an {@link FxEffect}: a static shape drawn by a renderer (particles or
 * block/item displays), offset from the effect's root. If given a {@link FxMotion}, the part's
 * local transform is advanced tick by tick from wherever it currently is — so it can move
 * independently of the group without either of them ever being told a final destination.
 * <p>
 * Purely presentational — fx never runs gameplay logic (damage, etc.) itself. Code that needs to
 * react to where a part actually is (an ability doing its own hit detection, say) should keep a
 * reference to the part it built and read {@link #currentTransform()} from its own tick loop.
 */
public final class FxPart {

    private final FxShape shape;
    private final FxRenderer renderer;
    private FxTransform transform = FxTransform.IDENTITY;
    private FxMotion motion;
    private IntPredicate activeTicks = tick -> true;
    private FxTransform worldTransform = FxTransform.IDENTITY;

    public FxPart(FxShape shape, FxRenderer renderer) {
        this.shape = shape;
        this.renderer = renderer;
    }

    /** Fixed starting offset of this part relative to the effect's root transform. */
    public FxPart transform(FxTransform transform) {
        this.transform = transform;
        return this;
    }

    /** Advances this part's local transform every tick, on top of the effect's root. */
    public FxPart motion(FxMotion motion) {
        this.motion = motion;
        return this;
    }

    /** Restricts rendering to ticks matching the predicate — e.g. a burst that should only draw once, or a trail thinned to every few ticks. Motion still advances every tick regardless. */
    public FxPart activeTicks(IntPredicate predicate) {
        this.activeTicks = predicate;
        return this;
    }

    /** Renders only on the first tick — for one-shot bursts (a flash, an impact) instead of a continuous effect. */
    public FxPart once() {
        return activeTicks(tick -> tick == 0);
    }

    /** This part's last-rendered position/rotation/scale in world space. Read-only; updated every tick. */
    public FxTransform currentTransform() {
        return worldTransform;
    }

    /** Sets the initial world transform before the first tick runs, so {@link #currentTransform()} is never stale/identity. */
    void primeWorldTransform(FxTransform root) {
        this.worldTransform = root.combine(transform);
    }

    void tick(World world, FxTransform root, int tick, Collection<Player> viewers) {
        if (motion != null) transform = motion.step(transform, tick);
        worldTransform = root.combine(transform);
        if (activeTicks.test(tick)) {
            renderer.render(world, worldTransform, shape.points(tick), tick, viewers);
        }
    }

    void remove() {
        renderer.remove();
    }
}
