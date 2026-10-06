package com.roguesmp.block.impl.generator.stat;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public record StatModifier(GeneratorStat stat, ModifierOperation operation, double amount) {

    public static final Codec<StatModifier> CODEC = Codec.composite(
            Codec.enumOf(GeneratorStat.class).fieldOf("stat").forGetter(StatModifier::stat),
            Codec.enumOf(ModifierOperation.class).optionalFieldOf("operation", ModifierOperation.MULTIPLY_BASE).forGetter(StatModifier::operation),
            Codec.DOUBLE.fieldOf("amount").forGetter(StatModifier::amount),
            StatModifier::new
    );

    public Component describe() {
        return Utils.text(operation.format(amount) + " " + stat.label(), color());
    }

    private NamedTextColor color() {
        return (amount > 0) == stat.higherIsBetter() ? NamedTextColor.GREEN : NamedTextColor.RED;
    }
}
