package com.roguesmp.block.storage;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class FixedSlots extends SlotGroup {

    private static final Codec<Map<String, StoredItem>> CODEC = Codec.withAlternative(
            Codec.unboundedMap(StoredItem.CODEC),
            StoredItem.CODEC.xmap(single -> Map.of("0", single), map -> map.get("0"))
    );

    private final StoredItem[] slots;

    public FixedSlots(String id, int size, SlotAccess access) {
        this(id, size, access, item -> true);
    }

    public FixedSlots(String id, int size, SlotAccess access, Predicate<StoredItem> filter) {
        super(id, access, filter);
        this.slots = new StoredItem[size];
    }

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public @Nullable StoredItem get(int slot) {
        return slots[slot];
    }

    public void set(int slot, @Nullable StoredItem item) {
        slots[slot] = item == null || item.amount() <= 0 ? null : item;
        changed();
    }

    @Override
    public int insert(StoredItem item, boolean simulate) {
        int accepted = insertInto(simulate ? slots.clone() : slots, item);
        if (accepted > 0 && !simulate) changed();
        return accepted;
    }

    @Override
    public int insert(int slot, StoredItem item, boolean simulate) {
        if (!accepts(item)) return 0;

        StoredItem existing = slots[slot];
        if (existing != null && !existing.item().equals(item.item())) return 0;

        int current = existing == null ? 0 : existing.amount();
        int accepted = Math.max(0, Math.min(item.amount(), item.maxStackSize() - current));
        if (accepted > 0 && !simulate) {
            slots[slot] = item.withAmount(current + accepted);
            changed();
        }
        return accepted;
    }

    /** Whether every item in {@code items} would fit at once. */
    public boolean canFitAll(List<StoredItem> items) {
        StoredItem[] simulated = slots.clone();
        for (StoredItem item : items) {
            if (insertInto(simulated, item) < item.amount()) return false;
        }
        return true;
    }

    private int insertInto(StoredItem[] target, StoredItem item) {
        if (!accepts(item)) return 0;

        int remaining = item.amount();
        int maxStack = item.maxStackSize();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < target.length && remaining > 0; i++) {
                StoredItem existing = target[i];
                boolean matching = existing != null && existing.item().equals(item.item());
                if (pass == 0 ? !matching : existing != null) continue;

                int current = existing == null ? 0 : existing.amount();
                int added = Math.min(remaining, maxStack - current);
                if (added <= 0) continue;
                target[i] = item.withAmount(current + added);
                remaining -= added;
            }
        }
        return item.amount() - remaining;
    }

    @Override
    public @Nullable StoredItem extract(int slot, int amount, boolean simulate) {
        StoredItem stored = slots[slot];
        if (stored == null || amount <= 0) return null;

        int taken = Math.min(amount, stored.amount());
        if (!simulate) {
            slots[slot] = stored.amount() > taken ? stored.withAmount(stored.amount() - taken) : null;
            changed();
        }
        return stored.withAmount(taken);
    }

    @Override
    public List<StoredItem> drain() {
        List<StoredItem> drained = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) drained.add(slots[i]);
            slots[i] = null;
        }
        if (!drained.isEmpty()) changed();
        return drained;
    }

    @Override
    protected List<StoredItem> defaultDemand() {
        List<StoredItem> demand = new ArrayList<>();
        for (StoredItem stored : slots) {
            if (stored == null) continue;
            int missing = stored.maxStackSize() - stored.amount();
            if (missing > 0) demand.add(stored.withAmount(missing));
        }
        return demand;
    }

    @Override
    StateSection<?> section() {
        return StateSection.of(CODEC.optionalFieldOf(id(), Map.of()), this::encode, this::decode);
    }

    private Map<String, StoredItem> encode() {
        Map<String, StoredItem> encoded = new LinkedHashMap<>();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) encoded.put(String.valueOf(i), slots[i]);
        }
        return encoded;
    }

    private void decode(Map<String, StoredItem> encoded) {
        Arrays.fill(slots, null);
        encoded.forEach((key, item) -> {
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < slots.length) slots[index] = item;
            } catch (NumberFormatException ignored) {
            }
        });
    }
}
