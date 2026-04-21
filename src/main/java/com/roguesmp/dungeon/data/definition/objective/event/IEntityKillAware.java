package com.roguesmp.dungeon.data.definition.objective.event;

/**
 * Receives entity-kill signals from the dungeon runtime.
 */
public interface IEntityKillAware {
    void onEntityKilled(String mobId);
}
