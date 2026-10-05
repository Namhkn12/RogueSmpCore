package com.roguesmp.block.impl.generator.module;

import com.roguesmp.block.impl.generator.module.behavior.GeneratorBehavior;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import net.kyori.adventure.text.Component;

import java.util.List;

public record GeneratorEffect(List<StatModifier> modifiers, List<GeneratorBehavior> behaviors) {

    public static final MapCodec<GeneratorEffect> CODEC = Codec.composite(
            Codec.listOf(StatModifier.CODEC).optionalFieldOf("modifiers", List.of()).forGetter(GeneratorEffect::modifiers),
            Codec.listOf(GeneratorBehavior.CODEC).optionalFieldOf("behaviors", List.of()).forGetter(GeneratorEffect::behaviors),
            GeneratorEffect::new
    );

    public List<Component> describe() {
        List<Component> lines = ModifierLore.describe(modifiers);
        behaviors.forEach(behavior -> lines.addAll(behavior.getDisplay()));
        return lines;
    }
}
