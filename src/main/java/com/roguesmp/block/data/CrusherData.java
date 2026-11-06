package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record CrusherData(int tier, double energyPerTick, int maxEnergy, double speed, int moduleSlots) {

    public static final CrusherData DEFAULT = new CrusherData(1, 2, 2000, 1.0, 3);

    public static final Codec<CrusherData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("tier", DEFAULT.tier).forGetter(CrusherData::tier),
            Codec.DOUBLE.optionalFieldOf("energy_per_tick", DEFAULT.energyPerTick).forGetter(CrusherData::energyPerTick),
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy).forGetter(CrusherData::maxEnergy),
            Codec.DOUBLE.optionalFieldOf("speed", DEFAULT.speed).forGetter(CrusherData::speed),
            Codec.INT.optionalFieldOf("module_slots", DEFAULT.moduleSlots).forGetter(CrusherData::moduleSlots),
            CrusherData::new
    );
}
