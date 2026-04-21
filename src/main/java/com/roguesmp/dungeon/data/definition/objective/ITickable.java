package com.roguesmp.dungeon.data.definition.objective;

/**
 * Objective that requires periodic ticking by the runtime loop.
 */
public interface ITickable {
    void tick();
}
