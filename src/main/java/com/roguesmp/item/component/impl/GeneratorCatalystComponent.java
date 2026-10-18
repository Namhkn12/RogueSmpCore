package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.block.impl.generator.part.DurationUnit;
import com.roguesmp.block.impl.generator.catalyst.CatalystType;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record GeneratorCatalystComponent(
        GeneratorEffect effect,
        int duration,
        DurationUnit unit
) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;

    public static final Codec<GeneratorCatalystComponent> CODEC = Codec.composite(
            GeneratorEffect.CODEC.forGetter(GeneratorCatalystComponent::effect),
            Codec.INT.fieldOf("duration").forGetter(GeneratorCatalystComponent::duration),
            Codec.enumOf(DurationUnit.class).optionalFieldOf("unit", DurationUnit.TICKS).forGetter(GeneratorCatalystComponent::unit),
            GeneratorCatalystComponent::new
    );

    public static @Nullable GeneratorCatalystComponent of(String itemId) {
        BaseItem item = Registries.ITEM.get(itemId);
        return item == null ? null : item.getComponent(ItemComponentKeys.GENERATOR_CATALYST);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>();
        lines.addAll(effect.describe());
        lines.add(Utils.text("Kéo dài: " + unit.format(duration), NamedTextColor.GRAY));
        String itemId = context.smpItem().getBaseItem().getId();
        List<String> typeNames = Registries.CATALYST_TYPE.getAll().values().stream()
                .filter(type -> type.matches(itemId))
                .map(CatalystType::displayText)
                .toList();
        if (!typeNames.isEmpty()) {
            lines.add(Utils.text("Loại chất xúc tác: ", NamedTextColor.GRAY).append(Utils.fromString(String.join(", ", typeNames))));
        }
        context.builder().putLines(LORE_PRIORITY, lines);
    }
}
