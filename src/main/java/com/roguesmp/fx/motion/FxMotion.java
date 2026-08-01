package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;

/**
 * Computes the next transform from the current one, one server tick at a time. Nothing about the
 * destination or overall path is declared up front — motions only ever know "where am I now,"
 * which is what lets them react to live state (a moving target, external input, etc.) each tick.
 */
@FunctionalInterface
public interface FxMotion {

    FxTransform step(FxTransform current, int tick);

    /** Chains another motion to run immediately after this one, same tick. */
    default FxMotion andThen(FxMotion next) {
        return (current, tick) -> next.step(this.step(current, tick), tick);
    }
}
