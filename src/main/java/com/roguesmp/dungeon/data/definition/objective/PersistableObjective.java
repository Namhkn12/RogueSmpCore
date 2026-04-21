package com.roguesmp.dungeon.data.definition.objective;

import java.util.Map;

/**
 * Allows objective runtime state to be saved and restored.
 */
public interface PersistableObjective {
    Map<String, Object> serialize();
    void deserialize(Map<String, Object> data);
}
