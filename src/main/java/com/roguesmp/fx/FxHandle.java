package com.roguesmp.fx;

/** Handle to a playing {@link FxEffect}, returned by {@link FxEngine#play(FxEffect)}. */
public final class FxHandle {

    private final FxEffect effect;

    FxHandle(FxEffect effect) {
        this.effect = effect;
    }

    public void stop() {
        FxEngine.getInstance().stop(effect);
    }

    public boolean isActive() {
        return !effect.isRemoved();
    }

    /** The effect's last-computed root transform in world space. See {@link FxEffect#currentRootTransform()}. */
    public FxTransform currentTransform() {
        return effect.currentRootTransform();
    }
}
