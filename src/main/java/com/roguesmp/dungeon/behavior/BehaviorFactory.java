package com.roguesmp.dungeon.behavior;

import com.roguesmp.dungeon.behavior.bhv.ProtectorSpawner;

public class BehaviorFactory {
    public static IBehavior create(BehaviorData data) {
        return switch (data.getType()) {
            case "protector"  -> new ProtectorSpawner(data);
            // thêm case ở đây khi có behavior mới
            default -> throw new IllegalArgumentException("Unknown behavior: " + data.getType());
        };
    }
}
