package com.roguesmp.dungeon_v2.data_.objective_.factory_;

import com.roguesmp.dungeon_v2.data_.objective_.IObjective;
import com.roguesmp.dungeon_v2.data_.objective_.PersistableObjective;
import com.roguesmp.dungeon_v2.data_.objective_.impl_.ItemCollector;
import com.roguesmp.dungeon_v2.data_.objective_.impl_.MonsterHunter;
import com.roguesmp.dungeon_v2.data_.objective_.impl_.SpawnerBreaker;

import java.util.Map;

public class ObjectiveFactory {

    public static IObjective create(ObjectiveConfig config) {
        return switch (config.getType()) {

            case SpawnerBreaker.TYPE -> {
                SpawnerBreaker obj = new SpawnerBreaker();
                obj.setRequire(config.getIntParam("required", 3));
                yield obj;
            }

            case MonsterHunter.TYPE -> {
                MonsterHunter obj = new MonsterHunter();
                obj.setRequire(config.getIntParam("required", 10));
                obj.setTargetIds(config.getStringListParam("targets"));
                yield obj;
            }

            case ItemCollector.TYPE -> {
                ItemCollector obj = new ItemCollector();
                obj.setRequire(config.getIntMapParam("require"));
                yield obj;
            }

            default -> throw new IllegalArgumentException("Unknown objective type: " + config.getType());
        };
    }
    /**
     * Dùng khi restore từ disk — data đã có state (count, completed).
     */
    public static IObjective restore(Map<String, Object> data) {
        String type = (String) data.get("type");
        ObjectiveConfig config = new ObjectiveConfig(type, data);
        IObjective obj = create(config);
        if (obj instanceof PersistableObjective p) {
            p.deserialize(data);
        }
        return obj;
    }
}