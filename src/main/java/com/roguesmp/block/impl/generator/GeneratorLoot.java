package com.roguesmp.block.impl.generator;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorLoot {

    private static final MapCodec<List<StoredItem>> CODEC = Codec.listOf(StoredItem.CODEC).optionalFieldOf("loot", List.of());

    private final List<StoredItem> entries = new ArrayList<>();

    public int occupied() {
        int total = 0;
        for (StoredItem stored : entries) total += stored.amount();
        return total;
    }

    /**
     * Every held entry by id and raw amount, which can exceed one real stack - callers must clamp via
     * {@link StoredItem#maxStackSize()} before resolving one to an ItemStack.
     */
    public List<StoredItem> entries() {
        return List.copyOf(entries);
    }

    public @Nullable StoredItem store(StoredItem rolled, int capacity) {
        int toStore = Math.max(0, Math.min(rolled.amount(), capacity - occupied()));
        if (toStore > 0) add(rolled.withAmount(toStore));

        int overflow = rolled.amount() - toStore;
        return overflow > 0 ? rolled.withAmount(overflow) : null;
    }

    /** Removes up to {@code amount} of {@code item}, or returns null if that id isn't stored. */
    public @Nullable StoredItem take(String item, int amount) {
        for (int i = 0; i < entries.size(); i++) {
            StoredItem stored = entries.get(i);
            if (!stored.item().equals(item)) continue;

            int taken = Math.min(amount, stored.amount());
            int remaining = stored.amount() - taken;
            if (remaining > 0) entries.set(i, stored.withAmount(remaining));
            else entries.remove(i);
            return new StoredItem(item, taken);
        }
        return null;
    }

    public List<StoredItem> drain() {
        List<StoredItem> drained = List.copyOf(entries);
        entries.clear();
        return drained;
    }

    public StateSection<List<StoredItem>> section() {
        return StateSection.of(CODEC, () -> entries, value -> {
            entries.clear();
            entries.addAll(value);
        });
    }

    private void add(StoredItem stored) {
        for (int i = 0; i < entries.size(); i++) {
            StoredItem existing = entries.get(i);
            if (!existing.item().equals(stored.item())) continue;

            entries.set(i, existing.withAmount(existing.amount() + stored.amount()));
            return;
        }
        entries.add(stored);
    }
}
