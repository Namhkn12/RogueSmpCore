package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.block.impl.generator.part.BurnUnit;
import com.roguesmp.block.impl.generator.fuel.FuelType;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record GeneratorFuelComponent(
        int energy,
        GeneratorEffect effect,
        int duration,
        BurnUnit unit
) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;

    public static final Codec<GeneratorFuelComponent> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("energy", 0).forGetter(GeneratorFuelComponent::energy),
            GeneratorEffect.CODEC.forGetter(GeneratorFuelComponent::effect),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(GeneratorFuelComponent::duration),
            Codec.enumOf(BurnUnit.class).optionalFieldOf("unit", BurnUnit.TICKS).forGetter(GeneratorFuelComponent::unit),
            GeneratorFuelComponent::new
    );

    public static @Nullable GeneratorFuelComponent of(String itemId) {
        BaseItem item = Registries.ITEM.get(itemId);
        return item == null ? null : item.getComponent(ItemComponentKeys.GENERATOR_FUEL);
    }

    public boolean hasEffect() {
        return duration > 0;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>();
        if (energy > 0) lines.add(Utils.text("Năng lượng: +" + energy, NamedTextColor.YELLOW));
        if (hasEffect()) {
            lines.addAll(effect.describe());
            lines.add(Utils.text("Kéo dài: " + unit.format(duration), NamedTextColor.GRAY));
        }
        String itemId = context.smpItem().getBaseItem().getId();
        List<String> typeNames = Registries.FUEL_TYPE.getAll().values().stream()
                .filter(type -> type.matches(itemId))
                .map(FuelType::displayText)
                .toList();
        if (!typeNames.isEmpty()) {
            lines.add(Utils.text("Loại nhiên liệu: ", NamedTextColor.GRAY).append(Utils.fromString(String.join(", ", typeNames))));
        }
        context.builder().putLines(LORE_PRIORITY, lines);
    }
}
