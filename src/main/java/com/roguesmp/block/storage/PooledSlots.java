package com.roguesmp.block.storage;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public final class PooledSlots extends SlotGroup {

    private static final Codec<Map<String, Integer>> CODEC = Codec.withAlternative(
            Codec.unboundedMap(Codec.INT),
            Codec.listOf(StoredItem.CODEC).xmap(PooledSlots::fromLegacyList, PooledSlots::toLegacyList)
    );

    private final Map<String, Integer> entries = new LinkedHashMap<>();
    private final IntSupplier capacity;

    public PooledSlots(String id, IntSupplier capacity, SlotAccess access) {
        super(id, access, item -> true);
        this.capacity = capacity;
    }

    public int occupied() {
        int total = 0;
        for (int amount : entries.values()) total += amount;
        return total;
    }

    public int capacity() {
        return capacity.getAsInt();
    }

    /** Every held entry by raw amount, which can exceed one real stack. */
    public List<StoredItem> entries() {
        return toLegacyList(entries);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public @Nullable StoredItem get(int slot) {
        int index = 0;
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            if (index++ == slot) return new StoredItem(entry.getKey(), entry.getValue());
        }
        return null;
    }

    @Override
    public int insert(StoredItem item, boolean simulate) {
        if (!accepts(item)) return 0;

        int accepted = Math.max(0, Math.min(item.amount(), capacity() - occupied()));
        if (accepted > 0 && !simulate) {
            entries.merge(item.item(), accepted, Integer::sum);
            changed();
        }
        return accepted;
    }

    @Override
    public int insert(int slot, StoredItem item, boolean simulate) {
        return insert(item, simulate);
    }

    @Override
    public @Nullable StoredItem extract(int slot, int amount, boolean simulate) {
        StoredItem stored = get(slot);
        return stored == null ? null : take(stored.item(), amount, simulate);
    }

    public @Nullable StoredItem take(String item, int amount, boolean simulate) {
        Integer stored = entries.get(item);
        if (stored == null || amount <= 0) return null;

        int taken = Math.min(amount, stored);
        if (!simulate) {
            if (stored > taken) entries.put(item, stored - taken);
            else entries.remove(item);
            changed();
        }
        return new StoredItem(item, taken);
    }

    @Override
    public List<StoredItem> drain() {
        List<StoredItem> drained = entries();
        entries.clear();
        if (!drained.isEmpty()) changed();
        return drained;
    }

    @Override
    protected List<StoredItem> defaultDemand() {
        return List.of();
    }

    @Override
    StateSection<?> section() {
        return StateSection.of(CODEC.optionalFieldOf(id(), Map.of()), () -> entries, value -> {
            entries.clear();
            entries.putAll(value);
        });
    }

    private static Map<String, Integer> fromLegacyList(List<StoredItem> list) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (StoredItem stored : list) map.merge(stored.item(), stored.amount(), Integer::sum);
        return map;
    }

    private static List<StoredItem> toLegacyList(Map<String, Integer> map) {
        List<StoredItem> list = new ArrayList<>(map.size());
        map.forEach((item, amount) -> list.add(new StoredItem(item, amount)));
        return list;
    }
}
