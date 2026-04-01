package com.roguesmp.dungeon_v2.data.definition.spawner.behavior;


import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.AlertSpawner;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.ProtectorSpawner;

import java.util.List;
import java.util.stream.Collectors;

public class BehaviorFactory {

    private BehaviorFactory() {}

    /** Tạo một behavior đơn từ BehaviorData */
    public static IBehavior create(BehaviorData data) {
        return switch (data.getType()) {
            case "protector" -> new ProtectorSpawner(data);
            case "alert"     -> new AlertSpawner(data);
            default -> throw new IllegalArgumentException(
                    "Unknown behavior type: '" + data.getType() + "'"
            );
        };
    }

    /** Tạo pipeline từ danh sách config */
    public static BehaviorPipeline createPipeline(List<BehaviorData> dataList) {
        List<IBehavior> behaviors = dataList.stream()
                .map(BehaviorFactory::create)
                .collect(Collectors.toList());
        return new BehaviorPipeline(behaviors);
    }
}