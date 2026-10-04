package com.roguesmp.block.data;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.codec.Codec;

import java.util.List;

public record GeneratorData(String place, int placeDelayTicks, int breakDelayTicks, List<BlockDrop> drops,
                            int maxEnergy, int energyPerTick, int energyPerFuel, int lootCapacity) {

    public static final GeneratorData DEFAULT = new GeneratorData(
            "minecraft:stone",
            100,
            100,
            List.of(),
            5000,
            1,
            200,
            1000
    );

    public static final Codec<GeneratorData> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("place", DEFAULT.place()).forGetter(GeneratorData::place),
            Codec.INT.optionalFieldOf("place_delay", DEFAULT.placeDelayTicks()).forGetter(GeneratorData::placeDelayTicks),
            Codec.INT.optionalFieldOf("break_delay", DEFAULT.breakDelayTicks()).forGetter(GeneratorData::breakDelayTicks),
            Codec.listOf(BlockDrop.CODEC).optionalFieldOf("drops", DEFAULT.drops()).forGetter(GeneratorData::drops),
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy()).forGetter(GeneratorData::maxEnergy),
            Codec.INT.optionalFieldOf("energy_per_tick", DEFAULT.energyPerTick()).forGetter(GeneratorData::energyPerTick),
            Codec.INT.optionalFieldOf("energy_per_fuel", DEFAULT.energyPerFuel()).forGetter(GeneratorData::energyPerFuel),
            Codec.INT.optionalFieldOf("capacity", DEFAULT.lootCapacity()).forGetter(GeneratorData::lootCapacity),
            GeneratorData::new
    );
}
