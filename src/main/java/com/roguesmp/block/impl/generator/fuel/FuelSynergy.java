package com.roguesmp.block.impl.generator.fuel;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;

public record FuelSynergy(Holder<FuelType> type, GeneratorEffect effect) {

    public static final Codec<FuelSynergy> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.FUEL_TYPE).fieldOf("type").forGetter(FuelSynergy::type),
            GeneratorEffect.CODEC.forGetter(FuelSynergy::effect),
            FuelSynergy::new
    );

    public boolean appliesTo(String fuelItemId) {
        return type.isBound() && type.value().matches(fuelItemId);
    }
}
