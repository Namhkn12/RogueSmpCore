package com.roguesmp.loot.entry;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.context.LootContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    protected @NotNull List<ItemStack> generate(@NotNull LootContext context, int depth) {
        return List.of();
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }
}
