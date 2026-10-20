package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

/**
 * @param potionItem item id of the produced potion, must have a {@code brewed_potion} component
 */
public record BrewingData(int maxEnergy, int energyPerTick, int brewTicks, String potionItem) {

    public static final BrewingData DEFAULT = new BrewingData(10000, 10, 200, "brewed_potion");

    public static final Codec<BrewingData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy()).forGetter(BrewingData::maxEnergy),
            Codec.INT.optionalFieldOf("energy_per_tick", DEFAULT.energyPerTick()).forGetter(BrewingData::energyPerTick),
            Codec.INT.optionalFieldOf("brew_ticks", DEFAULT.brewTicks()).forGetter(BrewingData::brewTicks),
            Codec.STRING.optionalFieldOf("potion_item", DEFAULT.potionItem()).forGetter(BrewingData::potionItem),
            BrewingData::new
    );
}
