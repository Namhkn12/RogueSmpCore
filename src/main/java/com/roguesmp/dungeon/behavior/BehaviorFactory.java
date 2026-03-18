package com.roguesmp.dungeon.behavior;

public class BehaviorFactory {
    public static IBehavior create(BehaviorData data) {
        return switch (data.getType()) {
            case "WAVE_SPAWN"  -> new WaveSpawnBehavior(data);
            case "RAGE_ON_HIT" -> new RageOnHitBehavior(data);
            // thêm case ở đây khi có behavior mới
            default -> throw new IllegalArgumentException("Unknown behavior: " + data.getType());
        };
    }
}
