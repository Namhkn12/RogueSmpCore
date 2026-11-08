package com.roguesmp.enchant.infusion;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record InfusionDefinition(Enchants enchant, InfusionType type, List<InfusionLevel> levels, int order) {

    private static final Codec<Enchants> ENCHANT_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                Enchants enchant = Enchants.fromId(id);
                return enchant != null ? DataResult.success(enchant) : DataResult.error("Unknown enchant: " + id);
            },
            enchant -> enchant.getEnchant().getId()
    );

    public static final Codec<InfusionDefinition> CODEC = Codec.composite(
            ENCHANT_CODEC.fieldOf("enchant").forGetter(InfusionDefinition::enchant),
            Codec.enumOf(InfusionType.class).optionalFieldOf("type", InfusionType.BASIC).forGetter(InfusionDefinition::type),
            Codec.listOf(InfusionLevel.CODEC).fieldOf("levels").forGetter(InfusionDefinition::levels),
            Codec.INT.optionalFieldOf("order", 0).forGetter(InfusionDefinition::order),
            InfusionDefinition::new
    );

    public SmpEnchant smpEnchant() {
        return enchant.getEnchant();
    }

    public int maxLevel() {
        return levels.size();
    }

    public @Nullable InfusionLevel level(int level) {
        if (level < 1 || level > levels.size()) return null;
        return levels.get(level - 1);
    }

    public static List<InfusionDefinition> sorted(InfusionType type) {
        return Registries.INFUSION.getAll().entrySet().stream()
                .filter(entry -> entry.getValue().type() == type)
                .sorted(Comparator.<Map.Entry<String, InfusionDefinition>>comparingInt(entry -> entry.getValue().order())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .toList();
    }
}
