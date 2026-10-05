package com.roguesmp.item.component.impl;

import com.roguesmp.block.impl.generator.module.BurnUnit;
import com.roguesmp.block.impl.generator.module.FuelType;
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

    private static List<String> fuelTypeNames(String itemId) {
        return Registries.FUEL_TYPE.getAll().values().stream()
                .filter(type -> type.matches(itemId))
                .map(FuelType::displayText)
                .toList();
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>();
        if (energy > 0) lines.add(ModifierLore.line("Năng lượng: +" + energy, NamedTextColor.YELLOW));
        if (hasEffect()) {
            lines.addAll(effect.describe());
            lines.add(ModifierLore.line("Kéo dài: " + unit.format(duration), NamedTextColor.GRAY));
        }
        List<String> typeNames = fuelTypeNames(context.smpItem().getBaseItem().getId());
        if (!typeNames.isEmpty()) {
            lines.add(ModifierLore.line("Loại nhiên liệu: ", NamedTextColor.GRAY).append(Utils.fromString(String.join(", ", typeNames))));
        }
        context.builder().putLines(LORE_PRIORITY, lines);
    }
}
