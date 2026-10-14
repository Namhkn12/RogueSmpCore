package com.roguesmp.spawner.behavior;

import com.roguesmp.RogueSmpCore;

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
