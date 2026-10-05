package com.roguesmp.item.component.impl;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.impl.generator.module.FuelSynergy;
import com.roguesmp.block.impl.generator.module.GeneratorEffect;
import com.roguesmp.block.impl.generator.module.ModifierLore;
import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record GeneratorModuleComponent(
        GeneratorEffect effect,
        List<String> incompatible,
        int maxPerMachine,
        List<FuelSynergy> synergies
) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;

    public static final Codec<GeneratorModuleComponent> CODEC = Codec.composite(
            GeneratorEffect.CODEC.forGetter(GeneratorModuleComponent::effect),
            Codec.listOf(Codec.STRING).optionalFieldOf("incompatible", List.of()).forGetter(GeneratorModuleComponent::incompatible),
            Codec.INT.optionalFieldOf("max_per_machine", 0).forGetter(GeneratorModuleComponent::maxPerMachine),
            Codec.listOf(FuelSynergy.CODEC).optionalFieldOf("synergies", List.of()).forGetter(GeneratorModuleComponent::synergies),
            GeneratorModuleComponent::new
    );

    public static @Nullable GeneratorModuleComponent of(String itemId) {
        BaseItem item = Registries.ITEM.get(itemId);
        return item == null ? null : item.getComponent(ItemComponentKeys.GENERATOR_MODULE);
    }

    private Component incompatibleLine() {
        Component line = ModifierLore.line("Xung đột: ", NamedTextColor.DARK_RED);
        for (int i = 0; i < incompatible.size(); i++) {
            if (i > 0) line = line.append(Component.text(", ", NamedTextColor.DARK_RED));
            line = line.append(new StoredItem(incompatible.get(i), 1).displayName());
        }
        return line;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>(effect.describe());
        for (FuelSynergy synergy : synergies) {
            lines.add(ModifierLore.line("Với nhiên liệu ", NamedTextColor.GOLD).append(Utils.fromString(synergy.type().isBound() ? synergy.type().value().displayText() : synergy.type().getId())).append(Component.text(":", NamedTextColor.GOLD)));
            lines.addAll(synergy.effect().describe());
        }
        if (maxPerMachine > 0) lines.add(ModifierLore.line("Tối đa mỗi máy: " + maxPerMachine, NamedTextColor.GRAY));
        if (!incompatible.isEmpty()) lines.add(incompatibleLine());
        context.builder().putLines(LORE_PRIORITY, lines);
    }
}
