package com.roguesmp.item.ability;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.ability.impl.*;
import com.roguesmp.registry.Registries;

public class ItemAbilities {

    public static final Codec<UnyieldingEdge> UNYIELDING_EDGE =
            register(UnyieldingEdge.TYPE_KEY, UnyieldingEdge.CODEC);

    public static final Codec<Barking> BARKING =
            register(Barking.TYPE_KEY, Barking.CODEC);

    public static void loadClass() {

    }

    private static <T extends ItemAbility> Codec<T> register(String id, Codec<T> codec) {
        Registries.ITEM_ABILITY_CODEC.register(id, codec);
        return codec;
    }
}
