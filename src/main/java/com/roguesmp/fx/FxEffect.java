package com.roguesmp.fx;

import com.roguesmp.fx.motion.FxMotion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A group of {@link FxPart}s anchored to a world location and ticked together. If given a root
 * {@link FxMotion}, the whole group's transform is advanced tick by tick from wherever it
 * currently is — moving/rotating everyone together — while individual parts can layer their own
 * motion relative to it, or stay fixed in place. No path or destination is ever declared upfront.
 * <p>
 * Purely presentational, same as {@link FxPart} — this never runs gameplay logic itself. Read
 * {@link #currentRootTransform()} (or a part's {@link FxPart#currentTransform()}, if you kept a
 * reference to it) from your own tick loop if you need to react to where the effect actually is.
 * <p>
 * By default every part is visible to everyone nearby. Scope the whole effect to specific players
 * (a caster-only ability effect, say) via {@link Builder#viewers}/{@link Builder#viewer}.
 * <p>
 * Build with {@link #builder(Location)}, then play it via {@link FxEngine#play(FxEffect)}.
 */
public final class FxEffect {

    private final Location origin;
    private final List<FxPart> parts;
    private final FxMotion rootMotion;
    private final int durationTicks;
    private final Runnable onComplete;
    private final Collection<Player> viewers;

    private FxTransform rootTransform;
    private int tick = 0;
    private boolean removed = false;

    private FxEffect(Builder builder) {
        this.origin = builder.origin;
        this.parts = List.copyOf(builder.parts);
        this.rootMotion = builder.rootMotion;
        this.durationTicks = builder.durationTicks;
        this.onComplete = builder.onComplete;
        this.viewers = builder.viewers;
        this.rootTransform = FxTransform.at(origin);
        this.parts.forEach(part -> part.primeWorldTransform(rootTransform));
    }

    public static Builder builder(Location origin) {
        return new Builder(origin);
    }

    /** This effect's last-computed root position/rotation/scale in world space. Read-only; updated every tick. */
    public FxTransform currentRootTransform() {
        return rootTransform;
    }

    void tick() {
        if (removed) return;

        World world = origin.getWorld();
        if (world == null) { // world unloaded (e.g. Multiverse) out from under a running effect
            stop();
            return;
        }

        if (rootMotion != null) rootTransform = rootMotion.step(rootTransform, tick);

        for (FxPart part : parts) {
            part.tick(world, rootTransform, tick, viewers);
        }

        tick++;
        if (durationTicks >= 0 && tick >= durationTicks) {
            stop();
        }
    }

    void stop() {
        if (removed) return;
        removed = true;
        parts.forEach(FxPart::remove);
        if (onComplete != null) onComplete.run();
    }

    boolean isRemoved() {
        return removed;
    }

    public static final class Builder {
        private final Location origin;
        private final List<FxPart> parts = new ArrayList<>();
        private FxMotion rootMotion;
        private int durationTicks = -1;
        private Runnable onComplete;
        private Collection<Player> viewers;

        private Builder(Location origin) {
            this.origin = origin.clone();
        }

        public Builder part(FxPart part) {
            parts.add(part);
            return this;
        }

        /** Motion the whole effect advances every tick, relative to wherever it currently is. */
        public Builder rootMotion(FxMotion motion) {
            this.rootMotion = motion;
            return this;
        }

        /** Ticks after which the effect stops itself and cleans up. -1 (default) runs until stopped externally. */
        public Builder duration(int ticks) {
            this.durationTicks = ticks;
            return this;
        }

        public Builder onComplete(Runnable onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        /** Restricts every part of this effect to these players only. Unset (default) means visible to everyone nearby. */
        public Builder viewers(Collection<Player> viewers) {
            this.viewers = viewers == null ? null : List.copyOf(viewers);
            return this;
        }

        /** Shorthand for {@link #viewers} with a single player — the common "caster-only" case. */
        public Builder viewer(Player viewer) {
            return viewers(List.of(viewer));
        }

        public FxEffect build() {
            return new FxEffect(this);
        }
    }
}
