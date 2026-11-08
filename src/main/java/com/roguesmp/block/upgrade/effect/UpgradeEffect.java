package com.roguesmp.block.upgrade.effect;

import com.roguesmp.block.upgrade.stat.StatModifier;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public record UpgradeEffect(List<StatModifier> modifiers, List<UpgradeBehavior> behaviors) {

    public static final UpgradeEffect EMPTY = new UpgradeEffect(List.of(), List.of());

    public static final MapCodec<UpgradeEffect> CODEC = Codec.composite(
            Codec.listOf(StatModifier.CODEC).optionalFieldOf("modifiers", List.of()).forGetter(UpgradeEffect::modifiers),
            Codec.listOf(UpgradeBehavior.CODEC).optionalFieldOf("behaviors", List.of()).forGetter(UpgradeEffect::behaviors),
            UpgradeEffect::new
    );

    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        modifiers.forEach(modifier -> lines.add(modifier.describe()));
        behaviors.forEach(behavior -> lines.addAll(behavior.getDisplay()));
        return lines;
    }
}
