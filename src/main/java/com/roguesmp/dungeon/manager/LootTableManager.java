package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.LootTable;
import com.roguesmp.dungeon.dto.DataResult;
import com.roguesmp.dungeon.repository.ILootTableRepository;
import com.roguesmp.dungeon.utils.Log4Craft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LootTableManager {

    private final ILootTableRepository repository;

    // Primary cache: id → LootTable
    private final Map<String, LootTable> cache = new HashMap<>();

    // Quick lookup sets, rebuilt on each reload
    private Set<String> tablesWithBonusRolls = Collections.emptySet();

    public LootTableManager(@NotNull ILootTableRepository repository) {
        this.repository = repository;
    }

    /**
     * Clears the cache and reloads all loot tables from the repository.
     * Call this on plugin enable and on /reload.
     */
    public void reload() {
        cache.clear();
        tablesWithBonusRolls = Collections.emptySet();

        DataResult<Map<String, LootTable>> dataResult = repository.loadAll();

        if (!dataResult.getLogs().isEmpty()) {
            Log4Craft.debug("[LootTableManager] " + dataResult.getLogs().size() + " error(s) while loading loot tables:");
            dataResult.getLogs().forEach(err -> Log4Craft.debug("  - " + err));
        }

        Map<String, LootTable> loaded = dataResult.getResult();
        if (loaded == null || loaded.isEmpty()) {
            Log4Craft.debug("[LootTableManager] No loot tables loaded — check your loot_tables/ folder.");
            return;
        }

        cache.putAll(loaded);
        propagateBonusRolls();
        buildIndexes();

        Log4Craft.info("[LootTableManager] Cached " + cache.size() + " loot tables. "
                + tablesWithBonusRolls.size() + " have bonus rolls.");

        logSummary();
    }

    /**
     * Returns the loot table for the given ID, or null if not found.
     *
     * @param id e.g. {@code "rogue:dungeons/dungeon_a_reward"}
     */
    public @Nullable LootTable getTable(@NotNull String id) {
        return cache.get(id);
    }

    /**
     * Returns true if a loot table with the given ID exists in cache.
     */
    public boolean exists(@NotNull String id) {
        return cache.containsKey(id);
    }

    /**
     * Returns an immutable view of all cached loot tables.
     */
    public @NotNull @Unmodifiable Map<String, LootTable> getAllTables() {
        return Collections.unmodifiableMap(cache);
    }

    /**
     * Returns all table IDs that have bonus_rolls (directly or via a nested child).
     */
    public @NotNull @Unmodifiable Set<String> getTablesWithBonusRolls() {
        return tablesWithBonusRolls;
    }

    /**
     * Quick check: does the table benefit from LootRules (luck, looting, tier)?
     */
    public boolean hasBonusRolls(@NotNull String id) {
        return tablesWithBonusRolls.contains(id);
    }

    public int getCacheSize() {
        return cache.size();
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    /**
     * Propagates hasBonusRolls upward through the nested table tree.
     *
     * <p>A parent loot table that calls a child with bonus_rolls should also
     * be considered as having bonus_rolls — this mirrors how Monumenta does it.
     *
     * <p>Algorithm: for each table with bonus_rolls, walk up through all tables
     * that reference it (as a LOOT_TABLE entry) and mark them too.
     */
    private void propagateBonusRolls() {
        // Step 1: build reverse map — child id → set of parent ids
        Map<String, Set<String>> childToParents = new HashMap<>();

        for (LootTable table : cache.values()) {
            table.getPools().forEach(pool ->
                    pool.getEntries().forEach(entry -> {
                        if (entry.getNestedTableId() != null) {
                            childToParents
                                    .computeIfAbsent(entry.getNestedTableId(), k -> new java.util.HashSet<>())
                                    .add(table.getId());
                        }
                    })
            );
        }

        // Step 2: collect all tables that directly have bonus_rolls
        Set<String> toPropagate = cache.values().stream()
                .filter(LootTable::hasBonusRolls)
                .map(LootTable::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        // Step 3: BFS upward through parent references
        Set<String> propagated = new java.util.HashSet<>(toPropagate);
        java.util.Queue<String> queue = new java.util.LinkedList<>(toPropagate);

        while (!queue.isEmpty()) {
            String childId = queue.poll();
            Set<String> parents = childToParents.get(childId);
            if (parents != null) {
                for (String parentId : parents) {
                    if (propagated.add(parentId)) {
                        queue.add(parentId);
                    }
                }
            }
        }

        this.tablesWithBonusRolls = Collections.unmodifiableSet(propagated);
    }

    private void buildIndexes() {
        // Placeholder for future indexes (e.g. by tag, by dungeon id, etc.)
    }

    private void logSummary() {
        tablesWithBonusRolls.forEach(id -> Log4Craft.info("  + " + id));

        Log4Craft.info("[LootTableManager] Tables WITHOUT bonus rolls:");
        cache.keySet().stream()
                .filter(id -> !tablesWithBonusRolls.contains(id))
                .forEach(id -> Log4Craft.info("  - " + id));
    }
}
