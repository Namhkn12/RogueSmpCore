package com.roguesmp.dungeon.data.definition.objective;

/**
 * Callback invoked when an objective transitions into completion.
 */
@FunctionalInterface
public interface ObjCallback {
    void callback(IObjective objective);
}
