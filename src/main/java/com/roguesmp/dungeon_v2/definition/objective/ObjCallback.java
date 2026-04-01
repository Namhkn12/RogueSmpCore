package com.roguesmp.dungeon_v2.definition.objective;

/**
 * Callback invoked when an objective transitions into completion.
 */
@FunctionalInterface
public interface ObjCallback {
    void callback(IObjective objective);
}
