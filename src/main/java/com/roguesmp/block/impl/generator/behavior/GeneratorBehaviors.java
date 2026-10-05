package com.roguesmp.block.impl.generator.behavior;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class GeneratorBehaviors {

    public static final Codec<ConvertDropsBehavior> CONVERT_DROPS = register(ConvertDropsBehavior.TYPE_KEY, ConvertDropsBehavior.CODEC);

    public static void loadClass() {

    }

    private static <T extends GeneratorBehavior> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.GENERATOR_BEHAVIOR_CODEC.register(typeName, codec);
    }
}
