package com.roguesmp.worldboss.definition;

import com.roguesmp.codec.Codec;

public record PeriodicSpawn(int joinWindowSeconds, int intervalSeconds, int jitterSeconds,
                            int minOnlinePlayers) implements SpawnMode {

    public static final String TYPE_KEY = "periodic";
    public static final Codec<PeriodicSpawn> CODEC = Codec.composite(
            Codec.INT.fieldOf("join_window_seconds").forGetter(PeriodicSpawn::joinWindowSeconds),
            Codec.INT.fieldOf("interval_seconds").forGetter(PeriodicSpawn::intervalSeconds),
            Codec.INT.optionalFieldOf("jitter_seconds", 0).forGetter(PeriodicSpawn::jitterSeconds),
            Codec.INT.optionalFieldOf("min_online_players", 0).forGetter(PeriodicSpawn::minOnlinePlayers),
            PeriodicSpawn::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
