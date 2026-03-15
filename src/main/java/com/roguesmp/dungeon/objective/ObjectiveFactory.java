package com.roguesmp.dungeon.objective;

import com.roguesmp.dungeon.objective.obj.SpawnerBreakObj;

public class ObjectiveFactory {
    public static IObjective create(ObjectiveData data) {
        return switch (data.getType()) {
            case "SPAWNER_BREAK" -> new SpawnerBreakObj(data);
            default -> throw new IllegalArgumentException("Unknown objective: " + data.getType());
        };
    }
}
