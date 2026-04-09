package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.loot.LootContext;
import com.roguesmp.dungeon_v2.data.definition.loot.LootEntry;
import com.roguesmp.dungeon_v2.data.definition.loot.LootPool;
import com.roguesmp.dungeon_v2.data.definition.loot.LootTable;
import com.roguesmp.dungeon_v2.dto.loot.LootRollResult;
import com.roguesmp.dungeon_v2.manager.LootTableManager;
import com.roguesmp.dungeon_v2.service.ILootService;
import com.roguesmp.dungeon_v2.utils.Log4Craft;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootService implements ILootService {

    /** Maximum recursion depth for nested loot_table entries. */
    private static final int MAX_DEPTH = 8;

    private final LootTableManager lootTableManager;
    private final ItemRegistry itemRegistry;

    public LootService(
           LootTableManager lootTableManager,
           ItemRegistry itemRegistry
    ) {
        this.lootTableManager = lootTableManager;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public List<ItemStack> roll(String lootTableId, LootContext context) {
        List<ItemStack> results = new ArrayList<>();
        rollTable(lootTableId, context, results, 0);
        return results;
    }

    @Override
    public List<ItemStack> roll(String lootTableId) {
        return roll(lootTableId, LootContext.builder().build());
    }

    @Override
    public boolean exists(String lootTableId) {
        return lootTableManager.exists(lootTableId);
    }

    /**
     * Resolves a loot table by ID and rolls all its pools, collecting into {@code results}.
     *
     * @param depth current recursion depth — aborts if exceeds {@link #MAX_DEPTH}
     */
    private void rollTable(
            String tableId,
            LootContext context,
            List<ItemStack> results,
            int depth
    ) {
        if (depth > MAX_DEPTH) {
            Log4Craft.debug("[LootService] Max recursion depth reached at table '" + tableId
                    + "' — possible circular reference.");
            return;
        }

        LootTable table = lootTableManager.getTable(tableId);
        if (table == null) {
            Log4Craft.debug("[LootService] Loot table not found: '" + tableId + "'");
            return;
        }

        for (LootPool pool : table.getPools()) {
            rollPool(pool, table, context, results, depth);
        }
    }

    /**
     * Rolls a single pool. Computes total rolls (base + bonus), then picks
     * one weighted entry per roll.
     */
    private void rollPool(
            LootPool pool,
            LootTable parentTable,
            LootContext context,
            List<ItemStack> results,
            int depth
    ) {
        int totalRolls = computeTotalRolls(pool, parentTable, context);

        for (int i = 0; i < totalRolls; i++) {
            LootEntry entry = pickWeightedEntry(pool);
            if (entry == null) continue;

            LootRollResult result = executeEntry(entry, context, depth);
            results.addAll(result.getItems());
        }
    }

    /**
     * Computes total rolls for a pool:
     * <pre>
     *   totalRolls = pool.rolls + floor(pool.bonusRolls * context.getBonusRollModifier())
     * </pre>
     *
     * <p>Bonus rolls are only applied if this table benefits from them (hasBonusRolls flag).
     * This avoids unnecessary rule evaluation on tables that ignore luck entirely.
     */
    private int computeTotalRolls(
            LootPool pool,
            LootTable parentTable,
            LootContext context
    ) {
        int base = Math.max(1, pool.getRolls());

        if (!pool.hasBonusRolls() || !lootTableManager.hasBonusRolls(parentTable.getId())) {
            return base;
        }

        double modifier = context.getBonusRollModifier();
        int bonus = (int) Math.floor(pool.getBonusRolls() * modifier);
        return base + Math.max(0, bonus);
    }

    /**
     * Picks one entry from the pool using weighted random selection.
     *
     * <p>Algorithm: sum all weights, pick a random value in [0, totalWeight),
     * walk entries subtracting weights until the value goes below zero.
     *
     * @return the selected entry, or null if the pool has no entries with positive weight
     */
    private LootEntry pickWeightedEntry(LootPool pool) {
        List<LootEntry> entries = pool.getEntries();
        if (entries.isEmpty()) return null;

        int totalWeight = entries.stream().mapToInt(LootEntry::getWeight).sum();
        if (totalWeight <= 0) {
            Log4Craft.debug("[LootService] Pool has zero total weight — skipping.");
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (LootEntry entry : entries) {
            roll -= entry.getWeight();
            if (roll < 0) return entry;
        }

        // Fallback (should not reach here)
        return entries.get(entries.size() - 1);
    }

    /**
     * Executes a single selected entry and returns the items it produces.
     */
    private LootRollResult executeEntry(
            LootEntry entry,
            LootContext context,
            int depth
    ) {
        return switch (entry.getType()) {
            case ITEM -> executeItemEntry(entry, context);
            case LOOT_TABLE -> executeNestedTableEntry(entry, context, depth);
            case EMPTY -> LootRollResult.empty();
        };
    }

    /**
     * Resolves an ITEM entry — looks up BaseItem via ItemRegistry,
     * picks a random amount in [min, max], builds the ItemStack via SmpItem.
     */
    private LootRollResult executeItemEntry(
            LootEntry entry,
            LootContext context
    ) {
        String itemId = entry.getItemId();
        if (itemId == null) {
            Log4Craft.debug("[LootService] ITEM entry has null item_id — skipping.");
            return LootRollResult.empty();
        }

        BaseItem baseItem = itemRegistry.getBaseItem(itemId);
        if (baseItem == null) {
            Log4Craft.debug("[LootService] Unknown item_id '" + itemId + "' — skipping.");
            return LootRollResult.empty();
        }

        int amount = randomAmount(entry.getMinAmount(), entry.getMaxAmount());
        ItemStack stack = baseItem.generateItemStack(context.getPlayer(), amount);

        return LootRollResult.of(stack);
    }

    /**
     * Resolves a LOOT_TABLE entry — recurses into the nested table.
     * Results are collected directly into the parent results list via the recursive call.
     */
    private LootRollResult executeNestedTableEntry(
            LootEntry entry,
            LootContext context,
            int depth
    ) {
        String nestedId = entry.getNestedTableId();
        if (nestedId == null) {
            Log4Craft.debug("[LootService] LOOT_TABLE entry has null nested table id — skipping.");
            return LootRollResult.empty();
        }

        List<ItemStack> nestedResults = new ArrayList<>();
        rollTable(nestedId, context, nestedResults, depth + 1);
        return LootRollResult.of(nestedResults);
    }

    private int randomAmount(int min, int max) {
        if (min >= max) return Math.max(1, min);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}