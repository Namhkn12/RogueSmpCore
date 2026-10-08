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

/**
 * Base type for a single entry inside a {@link LootPool}. Concrete kinds
 * ({@code com.roguesmp.loot.entry.ItemEntry}, {@code NestedTableEntry}, {@code EmptyEntry})
 * live in {@code com.roguesmp.loot.entry}, registered into {@link Registries#LOOT_ENTRY_CODEC}
 * (see {@code com.roguesmp.loot.entry.LootEntries}) — same polymorphic dispatch pattern as
 * {@code SmpEffect}/{@code ItemComponent} elsewhere in the plugin (see
 * {@code com.roguesmp.codec.INFO.md} section 7).
 *
 * <p>Every entry, regardless of kind, carries:
 * <ul>
 *   <li>{@code weight} — used for the weighted pick within its pool.</li>
 *   <li>{@code quality} — how strongly {@link com.roguesmp.loot.context.LootContext#getLuck()} scales
 *       this entry's weight: {@code weight * max(0, 1 + luck * quality)}. 0 = unaffected.</li>
 *   <li>{@code conditions} — every {@link LootCondition} must {@link LootCondition#test} true
 *       for the entry to even enter the weighted pick; a failing entry is excluded from the
 *       pool's weight sum entirely, not just zero-weighted.</li>
 *   <li>{@code functions} — every {@link LootFunction}, run in order, post-processes the
 *       {@link org.bukkit.inventory.ItemStack}s this entry produces once picked.</li>
 * </ul>
 */
public abstract class LootEntry {

    public static final Codec<LootEntry> CODEC = Codec.dispatch(LootEntry::getTypeId, Registries.LOOT_ENTRY_CODEC::getOrThrow);

    /** Fields shared by every entry kind. */
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
        // Defensive copy - Codec.listOf's decode returns plain mutable ArrayLists.
        this.base = new BaseProperties(base.weight(), base.quality(), List.copyOf(base.conditions()), List.copyOf(base.functions()));
    }

    /** JSON {@code "type"} dispatch key - {@code "item"}/{@code "loot_table"}/{@code "empty"} for the built-in kinds. */
    public abstract @NotNull String getTypeId();

    /**
     * Produces this entry's items, then runs its {@link LootFunction}s in order over them, each
     * function receiving the previous one's output.
     *
     * @param depth nested-table recursion depth of the roll this entry is picked in
     */
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
