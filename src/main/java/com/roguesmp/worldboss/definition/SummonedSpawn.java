package com.roguesmp.worldboss.definition;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.cost.Cost;

import java.util.List;

public record SummonedSpawn(int joinWindowSeconds, int cooldownSeconds,
                            List<Cost> costs) implements SpawnMode {

    public static final String TYPE_KEY = "summoned";
    public static final Codec<SummonedSpawn> CODEC = Codec.composite(
            Codec.INT.fieldOf("join_window_seconds").forGetter(SummonedSpawn::joinWindowSeconds),
            Codec.INT.optionalFieldOf("cooldown_seconds", 0).forGetter(SummonedSpawn::cooldownSeconds),
            Codec.listOf(Cost.CODEC).optionalFieldOf("costs", List.of()).forGetter(SummonedSpawn::costs),
            SummonedSpawn::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
