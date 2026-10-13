package com.roguesmp.dungeon_spawner.behavior;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon_spawner.behavior.impl.CursedSpawner;
import com.roguesmp.dungeon_spawner.behavior.impl.ProtectorSpawner;
import com.roguesmp.dungeon_spawner.behavior.impl.TeleportSpawner;
import com.roguesmp.dungeon_spawner.behavior.impl.WaveSpawner;

public class BehaviorFactory {

    private BehaviorFactory() {}

    public static BaseBehavior create(BehaviorData data) {
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
}
