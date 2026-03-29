package com.roguesmp.dungeon.objective_;

import com.roguesmp.dungeon.objective_.impl.MonsterSlayerObj;
import com.roguesmp.dungeon.objective_.impl.SpawnerBreakObj;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;

public class ObjectiveFactory {
    public static IObjective create(ObjectiveData data) {
        return switch (data.getType()) {
            case "spawner_break" -> new SpawnerBreakObj(data);
            case "monster_slayer" -> new MonsterSlayerObj(data);
            default -> throw new IllegalArgumentException("Unknown objective: " + data.getType());
        };
    }
}
