package com.roguesmp.dungeon_v2.data.definition.objective.factory;

import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.PersistableObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.impl.ItemCollector;
import com.roguesmp.dungeon_v2.data.definition.objective.impl.MonsterHunter;
import com.roguesmp.dungeon_v2.data.definition.objective.impl.SpawnerBreaker;

import java.util.Map;

/**
 * Creates objective runtime objects from serialized config/state.
 */
public final class ObjectiveFactory {

    private ObjectiveFactory() {
    }

    public static IObjective create(ObjectiveConfig config) {
        return switch (config.getType()) {
            case SpawnerBreaker.TYPE -> {
                SpawnerBreaker objective = new SpawnerBreaker();
                objective.setRequire(getRequiredCount(config, 3));
                yield objective;
            }
            case MonsterHunter.TYPE -> {
                MonsterHunter objective = new MonsterHunter();
                objective.setRequire(getRequiredCount(config, 10));
                objective.setTargetId(config.getStringParam("target", ""));
                yield objective;
            }
            case ItemCollector.TYPE -> {
                ItemCollector objective = new ItemCollector();
                objective.setRequire(config.getIntParam("require", 1));
                yield objective;
            }
            default -> throw new IllegalArgumentException("Unknown objective type: " + config.getType());
        };
    }

    public static IObjective restore(Map<String, Object> data) {
        String type = data.get("type") instanceof String value ? value : null;
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Missing objective type in serialized data.");
        }

        ObjectiveConfig config = new ObjectiveConfig(type, data);
        IObjective objective = create(config);
        if (objective instanceof PersistableObjective persistable) {
            persistable.deserialize(data);
        }
        return objective;
    }

    private static int getRequiredCount(ObjectiveConfig config, int defaultValue) {
        return config.getIntParam("require", config.getIntParam("required", defaultValue));
    }
}
