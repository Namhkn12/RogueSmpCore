package com.roguesmp.dungeon_v2.data.definition.objective;

/**
 * Objective that requires periodic ticking by the runtime loop.
 */
public interface ITickable {
    void tick();
}
