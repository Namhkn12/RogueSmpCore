package com.roguesmp.loot.entry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Delegates to another {@link LootTable} through a {@link Holder}, so a dangling table id is
 * reported by {@code Registry#validateAllHolders} right after load and survives registry reloads
 * (recursion depth capped in {@link LootTable#rollInto}, to guard against a circular reference).
 */
public final class NestedTableEntry extends LootEntry {

    public static final String TYPE_KEY = "loot_table";

    public static final Codec<NestedTableEntry> CODEC = Codec.composite(
            LootEntry.BASE_CODEC.forGetter(LootEntry::getBaseProperties),
            Registry.referenceCodec(() -> Registries.LOOT_TABLE).fieldOf("name").forGetter(NestedTableEntry::getNestedTable),
            NestedTableEntry::new
    );

    private final Holder<LootTable> nestedTable;

    public NestedTableEntry(@NotNull BaseProperties base, @NotNull Holder<LootTable> nestedTable) {
        super(base);
        this.nestedTable = nestedTable;
    }

    @Override
    protected @NotNull List<ItemStack> generate(@NotNull LootContext context, int depth) {
        if (!nestedTable.isBound()) {
            RogueSmpCore.LOGGER.warn("[LootTable] Loot table not found: '" + nestedTable.getId() + "'");
            return List.of();
        }

        List<ItemStack> results = new ArrayList<>();
        nestedTable.value().rollInto(context, results, depth + 1);
        return results;
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    public @NotNull Holder<LootTable> getNestedTable() {
        return nestedTable;
    }

    public @NotNull String getNestedTableId() {
        return nestedTable.getId();
    }
}
