package com.roguesmp.block.impl.generator.module;

import com.roguesmp.codec.Codec;

public record StatModifier(GeneratorStat stat, ModifierOperation operation, double amount) {

    public static final Codec<StatModifier> CODEC = Codec.composite(
            Codec.enumOf(GeneratorStat.class).fieldOf("stat").forGetter(StatModifier::stat),
            Codec.enumOf(ModifierOperation.class).optionalFieldOf("operation", ModifierOperation.MULTIPLY_BASE).forGetter(StatModifier::operation),
            Codec.DOUBLE.fieldOf("amount").forGetter(StatModifier::amount),
            StatModifier::new
    );
}
