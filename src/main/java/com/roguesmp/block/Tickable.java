package com.roguesmp.block;

/**
 * Implemented by an {@link SmpBlock} subclass that wants a per-tick callback. Only blocks
 * implementing this are added to {@code BlockManager}'s per-tick loop - a block that doesn't costs
 * nothing.
 */
public interface Tickable {

    /**
     * Called every server tick while the block is loaded and hydrated. Defer spawning/removing
     * other entities or blocks via the scheduler rather than doing it inline here, same as every
     * other {@link SmpBlock} hook.
     */
    void tick();
}
