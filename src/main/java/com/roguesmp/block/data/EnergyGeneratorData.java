package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;
import net.kyori.adventure.key.Key;

public record EnergyGeneratorData(int maxEnergy, int transferRate, Key modelActive) {

    public static final EnergyGeneratorData DEFAULT = new EnergyGeneratorData(10000, 20, Key.key("minecraft:stone"));

    public static final Codec<EnergyGeneratorData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy()).forGetter(EnergyGeneratorData::maxEnergy),
            Codec.INT.optionalFieldOf("transfer_rate", DEFAULT.transferRate()).forGetter(EnergyGeneratorData::transferRate),
            Codec.KEY.optionalFieldOf("model_active", DEFAULT.modelActive()).forGetter(EnergyGeneratorData::modelActive),
            EnergyGeneratorData::new
    );
}
