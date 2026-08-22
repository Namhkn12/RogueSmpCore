package com.roguesmp.loot.entry;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import org.jetbrains.annotations.NotNull;

/**
 * A pure "miss" - contributes {@code weight} to the pool but produces nothing when picked.
 */
public final class EmptyEntry extends LootEntry {

    public static final String TYPE_KEY = "empty";

    public static final Codec<EmptyEntry> CODEC = LootEntry.BASE_CODEC
            .xmap(EmptyEntry::new, EmptyEntry::getBaseProperties)
            .codec();

    public EmptyEntry(@NotNull BaseProperties base) {
        super(base);
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }
}
