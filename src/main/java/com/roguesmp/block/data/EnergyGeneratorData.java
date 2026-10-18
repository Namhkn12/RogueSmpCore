package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record EnergyGeneratorData(int maxEnergy, int transferRate) {

    public static final EnergyGeneratorData DEFAULT = new EnergyGeneratorData(10000, 20);

    public static final Codec<EnergyGeneratorData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy()).forGetter(EnergyGeneratorData::maxEnergy),
            Codec.INT.optionalFieldOf("transfer_rate", DEFAULT.transferRate()).forGetter(EnergyGeneratorData::transferRate),
            EnergyGeneratorData::new
    );
}
