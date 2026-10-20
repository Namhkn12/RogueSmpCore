package com.roguesmp.loot.entry;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.registry.Registries;

public class LootEntries {
    public static final Codec<ItemEntry> ITEM = register(ItemEntry.TYPE_KEY, ItemEntry.CODEC);
    public static final Codec<NestedTableEntry> LOOT_TABLE = register(NestedTableEntry.TYPE_KEY, NestedTableEntry.CODEC);
    public static final Codec<EmptyEntry> EMPTY = register(EmptyEntry.TYPE_KEY, EmptyEntry.CODEC);

    public static void loadClass() {
    }

    private static <T extends LootEntry> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.LOOT_ENTRY_CODEC.register(typeName, codec);
    }
}
