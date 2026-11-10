package com.roguesmp.worldboss.definition;

import com.roguesmp.codec.Codec;

public sealed interface SpawnMode permits SummonedSpawn, PeriodicSpawn {

    Codec<SpawnMode> CODEC = Codec.dispatch(
            SpawnMode::getTypeId,
            type -> switch (type) {
                case SummonedSpawn.TYPE_KEY -> SummonedSpawn.CODEC;
                case PeriodicSpawn.TYPE_KEY -> PeriodicSpawn.CODEC;
                default -> null;
            }
    );

    String getTypeId();

    int joinWindowSeconds();
}
