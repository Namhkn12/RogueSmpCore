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
