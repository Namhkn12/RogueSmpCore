package com.roguesmp.constant;

public enum DamageOperation {
    /**
     * Base damage operation, will override everything with the same BASE operation
     */
    BASE,
    /**
     * Adds flat damage before scaling
     */
    ADD_BASE,
    /**
     * % increased damage (additive stacking)
     */
    INCREASE_BASE,
    /**
     * Base multiplier (multiplicative stacking)
     */
    MORE_BASE,
    /**
     * Final multiplier (multiplicative stacking)
     */
    MORE_FINAL,
    /**
     * Flat damage added at the very end
     */
    ADD_FINAL,
}
