package com.roguesmp.block.impl.generator.catalyst;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;

public record CatalystSynergy(Holder<CatalystType> type, GeneratorEffect effect) {

    public static final Codec<CatalystSynergy> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.CATALYST_TYPE).fieldOf("type").forGetter(CatalystSynergy::type),
            GeneratorEffect.CODEC.forGetter(CatalystSynergy::effect),
            CatalystSynergy::new
    );

    public boolean appliesTo(String catalystItemId) {
        return type.isBound() && type.value().matches(catalystItemId);
    }
}
