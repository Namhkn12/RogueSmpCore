package com.roguesmp.dungeon_v2.data.definition.objective.factory;

import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.PersistableObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.impl.DemonSlayer;
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
        IObjective objective = switch (config.getType()) {
            case SpawnerBreaker.TYPE -> {
                SpawnerBreaker obj = new SpawnerBreaker();
                obj.setRequire(getRequiredCount(config, 3));
                yield obj;
            }
            case MonsterHunter.TYPE -> {
                MonsterHunter obj = new MonsterHunter();
                obj.setRequire(getRequiredCount(config, 10));
                obj.setTargetId(normalizeTarget(config.getStringParam("target", null)));
                yield obj;
            }
            case ItemCollector.TYPE -> {
                ItemCollector obj = new ItemCollector();
                obj.setRequire(config.getIntParam("require", 1));
                obj.setTargetItemId(normalizeTarget(config.getStringParam("target", null)));
                yield obj;
            }
            case DemonSlayer.TYPE -> {
                DemonSlayer obj = new DemonSlayer();
                obj.setRequire(getRequiredCount(config, 1));
                obj.setTargetId(normalizeTarget(config.getStringParam("target", null)));
                yield obj;
            }
            default -> throw new IllegalArgumentException("Unknown objective type: " + config.getType());
        };

        BaseObjective base = (BaseObjective) objective;
        base.setScore(config.getIntParam("score", 0));

        return objective;
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

    private static String normalizeTarget(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
