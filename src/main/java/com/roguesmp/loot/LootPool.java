package com.roguesmp.loot;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a single pool inside a {@link LootTable}.
 *
 * <p>When a pool is rolled:
 * <ol>
 *   <li>If any of {@code conditions} fails, the whole pool is skipped — no rolls happen at
 *       all, not even {@code EMPTY} ones.</li>
 *   <li>For each of {@code rolls} rolls, pick one entry by weighted random</li>
 *   <li>Resolve the chosen entry (drop item / delegate to nested table / do nothing)</li>
 * </ol>
 */
public class LootPool {

    public static final Codec<LootPool> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("rolls", 1).forGetter(LootPool::getRolls),
            Codec.listOf(LootEntry.CODEC).fieldOf("entries").forGetter(LootPool::getEntries),
            Codec.listOf(LootCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(LootPool::getConditions),
            LootPool::new
    );

    private final int rolls;
    private final @NotNull List<LootEntry> entries;
    private final @NotNull List<LootCondition> conditions;

    public LootPool(int rolls, @NotNull List<LootEntry> entries, @NotNull List<LootCondition> conditions) {
        this.rolls = rolls;
        this.entries = Collections.unmodifiableList(entries);
        this.conditions = Collections.unmodifiableList(conditions);
    }

    public int getRolls() {
        return rolls;
    }

    public @NotNull @Unmodifiable List<LootEntry> getEntries() {
        return entries;
    }

    /**
     * Gates the entire pool — see {@link LootCondition}. Unlike an entry's own conditions
     * (which only remove that one entry from the weighted pick), a failing pool condition
     * skips the pool outright: no rolls, no weighted pick, nothing added to the results.
     */
    public @NotNull @Unmodifiable List<LootCondition> getConditions() {
        return conditions;
    }

    public boolean isActive(@NotNull LootContext context) {
        return LootCondition.allPass(conditions, context);
    }

    public void roll(@NotNull LootContext context, @NotNull List<ItemStack> results, int depth) {
        if (!isActive(context)) return;

        int totalRolls = Math.max(1, rolls);
        for (int i = 0; i < totalRolls; i++) {
            LootEntry entry = pick(context);
            if (entry != null) results.addAll(entry.resolve(context, depth));
        }
    }

    /**
     * Effective drop chance of every entry that can currently be picked (luck applied, failing
     * entry conditions excluded) — what {@link #roll} samples from. Empty when the pool itself
     * is skipped.
     */
    public @NotNull List<LootOdds.EntryOdds> odds(@NotNull LootContext context) {
        if (!isActive(context)) return List.of();

        List<WeightedEntry> weighted = weigh(context);
        double totalWeight = totalWeight(weighted);

        return weighted.stream()
                .map(candidate -> new LootOdds.EntryOdds(candidate.entry(), candidate.weight(), candidate.weight() / totalWeight))
                .toList();
    }

    private @Nullable LootEntry pick(LootContext context) {
        List<WeightedEntry> weighted = weigh(context);
        double totalWeight = totalWeight(weighted);
        if (totalWeight <= 0) return null;

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (WeightedEntry candidate : weighted) {
            roll -= candidate.weight();
            if (roll < 0) return candidate.entry();
        }

        return weighted.get(weighted.size() - 1).entry();
    }

    private record WeightedEntry(LootEntry entry, double weight) {}

    /**
     * Effective weight of every entry whose own {@link LootCondition}s pass: a failing entry is
     * excluded entirely, the rest get {@code weight * max(0, 1 + luck * quality)} and drop out
     * if that comes to zero.
     */
    private List<WeightedEntry> weigh(LootContext context) {
        List<WeightedEntry> weighted = new ArrayList<>(entries.size());

        for (LootEntry entry : entries) {
            if (!LootCondition.allPass(entry.getConditions(), context)) continue;

            double weight = entry.getWeight() * Math.max(0, 1 + context.getLuck() * entry.getQuality());
            if (weight > 0) weighted.add(new WeightedEntry(entry, weight));
        }

        return weighted;
    }

    private static double totalWeight(List<WeightedEntry> weighted) {
        return weighted.stream().mapToDouble(WeightedEntry::weight).sum();
    }
}
