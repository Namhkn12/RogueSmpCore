package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record SieveData(int tier, double energyPerTick, int maxEnergy, double speed, int moduleSlots) {

    public static final SieveData DEFAULT = new SieveData(1, 2, 2000, 1.0, 3);

    public static final Codec<SieveData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("tier", DEFAULT.tier).forGetter(SieveData::tier),
            Codec.DOUBLE.optionalFieldOf("energy_per_tick", DEFAULT.energyPerTick).forGetter(SieveData::energyPerTick),
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy).forGetter(SieveData::maxEnergy),
            Codec.DOUBLE.optionalFieldOf("speed", DEFAULT.speed).forGetter(SieveData::speed),
            Codec.INT.optionalFieldOf("module_slots", DEFAULT.moduleSlots).forGetter(SieveData::moduleSlots),
            SieveData::new
    );
}
