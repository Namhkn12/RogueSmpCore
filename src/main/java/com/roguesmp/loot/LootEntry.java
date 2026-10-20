package com.roguesmp.loot;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.function.LootFunction;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public abstract class LootEntry {
    public static final Codec<LootEntry> CODEC = Codec.dispatch(LootEntry::getTypeId, Registries.LOOT_ENTRY_CODEC::getOrThrow);

    public record BaseProperties(int weight, double quality, List<LootCondition> conditions, List<LootFunction> functions) {}

    public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("weight", 1).forGetter(BaseProperties::weight),
            Codec.DOUBLE.optionalFieldOf("quality", 0.0).forGetter(BaseProperties::quality),
            Codec.listOf(LootCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(BaseProperties::conditions),
            Codec.listOf(LootFunction.CODEC).optionalFieldOf("functions", List.of()).forGetter(BaseProperties::functions),
            BaseProperties::new
    );

    private final BaseProperties base;

    protected LootEntry(@NotNull BaseProperties base) {
        this.base = new BaseProperties(base.weight(), base.quality(), List.copyOf(base.conditions()), List.copyOf(base.functions()));
    }

    public abstract @NotNull String getTypeId();

    public final @NotNull List<ItemStack> resolve(@NotNull LootContext context, int depth) {
        List<ItemStack> current = List.copyOf(generate(context, depth));
        for (LootFunction function : base.functions()) {
            current = List.copyOf(function.apply(current, context));
        }
        return current;
    }

    protected abstract @NotNull List<ItemStack> generate(@NotNull LootContext context, int depth);

    public @NotNull BaseProperties getBaseProperties() {
        return base;
    }

    public int getWeight() {
        return base.weight();
    }

    public double getQuality() {
        return base.quality();
    }

    public @NotNull @Unmodifiable List<LootCondition> getConditions() {
        return base.conditions();
    }

    public @NotNull @Unmodifiable List<LootFunction> getFunctions() {
        return base.functions();
    }
}
