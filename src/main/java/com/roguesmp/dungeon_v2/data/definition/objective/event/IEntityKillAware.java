package com.roguesmp.dungeon_v2.data.definition.objective.event;

/**
 * Receives entity-kill signals from the dungeon runtime.
 */
public interface IEntityKillAware {
    void onEntityKilled(String mobId);
}
