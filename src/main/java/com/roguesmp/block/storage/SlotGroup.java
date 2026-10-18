package com.roguesmp.block.storage;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A named group of item slots owned by a block. {@link #access()} is the automation rule
 * (what neighbours may push in or pull out) - players can always take items out through a GUI,
 * but can only put them in where automation could too. {@link #insert}/{@link #extract} themselves
 * only enforce the filter and stack rules, so the owning block can always fill its own outputs.
 */
public abstract class SlotGroup {

    private final String id;
    private final String displayName;
    private final SlotAccess access;
    private final Predicate<StoredItem> filter;
    private Runnable onChange = () -> {};
    private @Nullable Supplier<List<StoredItem>> demandOverride;

    protected SlotGroup(String id, String displayName, SlotAccess access, Predicate<StoredItem> filter) {
        this.id = id;
        this.displayName = displayName;
        this.access = access;
        this.filter = filter;
    }

    public abstract int size();

    public abstract @Nullable StoredItem get(int slot);

    /** Returns how many of {@code item} were (or, when simulating, would be) accepted. */
    public abstract int insert(StoredItem item, boolean simulate);

    public abstract int insert(int slot, StoredItem item, boolean simulate);

    public abstract @Nullable StoredItem extract(int slot, int amount, boolean simulate);

    public abstract List<StoredItem> drain();

    abstract StateSection<?> section();

    /** Items this group wants topped up by a smart pull, each with the amount still missing. */
    protected abstract List<StoredItem> defaultDemand();

    public List<StoredItem> demand() {
        if (!access.allowsInsert()) return List.of();

        List<StoredItem> overridden = demandOverride == null ? null : demandOverride.get();
        return overridden != null ? overridden : defaultDemand();
    }

    public boolean supportsDemand() {
        return access.allowsInsert() && (demandOverride != null || hasDefaultDemand());
    }

    protected boolean hasDefaultDemand() {
        return true;
    }

    public String displayName() {
        return displayName;
    }

    /** Replaces the top-up demand used by smart pulls; the supplier may return null to fall back to it. */
    public SlotGroup withDemand(Supplier<List<StoredItem>> demand) {
        this.demandOverride = demand;
        return this;
    }

    public boolean accepts(StoredItem item) {
        return filter.test(item);
    }

    public String id() {
        return id;
    }

    public SlotAccess access() {
        return access;
    }

    void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    protected void changed() {
        onChange.run();
    }
}
