package com.roguesmp.dungeon_v2.data.definition.spawner.behavior;


import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.CursedSpawner;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.ProtectorSpawner;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.TeleportSpawner;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl.WaveSpawner;

import java.util.List;
import java.util.stream.Collectors;

public class BehaviorFactory {

    private BehaviorFactory() {}

    /** Tạo một behavior đơn từ BehaviorData */
    public static IBehavior create(BehaviorData data) {
        return switch (data.getType()) {
            case "protector" -> new ProtectorSpawner(data);
            case "wave"     -> new WaveSpawner(data);
            case "teleport" -> new TeleportSpawner(data, RogueSmpCore.getInstance());
            case "cursed" -> new CursedSpawner(data);
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