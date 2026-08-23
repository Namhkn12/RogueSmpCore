package com.roguesmp.loot.function;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * Every {@link LootFunction} codec. Each constant registers itself into
 * {@link Registries#LOOT_FUNCTION_CODEC} as it's initialized — call {@link #loadClass()} to
 * force that to happen.
 */
public class LootFunctions {

    public static void loadClass() {

    }

    private static <T extends LootFunction> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.LOOT_FUNCTION_CODEC.register(typeName, codec);
    }
}
