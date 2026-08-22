package com.roguesmp.loot.entry;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Delegates to another {@code com.roguesmp.loot.LootTable} by id, resolved lazily at roll time
 * (recursion depth capped in {@code LootService}, to guard against a circular reference).
 */
public final class NestedTableEntry extends LootEntry {

    public static final String TYPE_KEY = "loot_table";

    public static final Codec<NestedTableEntry> CODEC = Codec.composite(
            LootEntry.BASE_CODEC.forGetter(LootEntry::getBaseProperties),
            Codec.STRING.fieldOf("name").forGetter(NestedTableEntry::getNestedTableId),
            NestedTableEntry::new
    );

    private final String nestedTableId;

    public NestedTableEntry(@NotNull BaseProperties base, @NotNull String nestedTableId) {
        super(base);
        this.nestedTableId = nestedTableId;
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    public @NotNull String getNestedTableId() {
        return nestedTableId;
    }
}
