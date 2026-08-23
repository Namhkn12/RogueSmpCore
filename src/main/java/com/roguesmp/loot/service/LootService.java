package com.roguesmp.loot.service;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.LootPool;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.LootRollResult;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.entry.ItemEntry;
import com.roguesmp.loot.entry.NestedTableEntry;
import com.roguesmp.loot.event.LootEntryResultEvent;
import com.roguesmp.loot.event.LootPoolPickEvent;
import com.roguesmp.loot.event.LootRollCompleteEvent;
import com.roguesmp.loot.event.LootRollEvent;
import com.roguesmp.loot.function.LootFunction;
import com.roguesmp.loot.manager.LootTableManager;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootService implements ILootService {

    private static @Nullable ILootService INSTANCE;

    /** Maximum recursion depth for nested loot_table entries. */
    private static final int MAX_DEPTH = 8;

    private final LootTableManager lootTableManager;

    public LootService(LootTableManager lootTableManager) {
        this.lootTableManager = lootTableManager;
    }

    /**
     * Plugin-wide singleton, wired once in {@code RogueSmpCore.init()} — anything (dungeon
     * chests, mob death, quests, ...) can roll loot without needing its own instance injected.
     */
    public static void init(LootTableManager lootTableManager) {
        INSTANCE = new LootService(lootTableManager);
    }

    public static ILootService getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("LootService is not initialized!");
        }
        return INSTANCE;
    }

    /**
     * Bắn {@link LootRollEvent} để mọi hệ thống khác kịp cộng modifier vào context,
     * sau đó mới roll. Chỉ bắn ở table gốc — table lồng nhau dùng lại context này.
     */
    @Override
    public List<ItemStack> roll(String lootTableId, LootContext context) {
        LootRollEvent event = new LootRollEvent(lootTableId, context);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return new ArrayList<>();

        List<ItemStack> results = new ArrayList<>();
        rollTable(lootTableId, context, results, 0);

        LootRollCompleteEvent completeEvent = new LootRollCompleteEvent(lootTableId, context, results);
        Bukkit.getPluginManager().callEvent(completeEvent);
        return completeEvent.getItems();
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
            RogueSmpCore.LOGGER.debug("[LootService] Max recursion depth reached at table '" + tableId
                    + "' — possible circular reference.");
            return;
        }

        LootTable table = lootTableManager.getTable(tableId);
        if (table == null) {
            RogueSmpCore.LOGGER.debug("[LootService] Loot table not found: '" + tableId + "'");
            return;
        }

        for (LootPool pool : table.getPools()) {
            rollPool(pool, table, context, results, depth);
        }
    }

    /**
     * Rolls a single pool. If the pool's own {@link LootCondition}s don't all pass, the pool is
     * skipped entirely — no rolls happen, not even {@code EMPTY} ones. Otherwise computes total
     * rolls (base + bonus), then picks one weighted entry per roll.
     */
    private void rollPool(
            LootPool pool,
            LootTable parentTable,
            LootContext context,
            List<ItemStack> results,
            int depth
    ) {
        if (!passesConditions(pool.getConditions(), context)) return;

        int totalRolls = computeTotalRolls(pool);

        for (int i = 0; i < totalRolls; i++) {
            LootEntry entry = pickWeightedEntry(pool, parentTable, context);
            if (entry == null) continue;

            LootRollResult result = executeEntry(entry, context, depth);
            List<ItemStack> items = applyFunctions(entry, result.getItems(), context);
            results.addAll(fireEntryResultEvent(entry, context, items));
        }
    }

    /**
     * Total rolls for a pool is just {@code max(1, pool.rolls)} — to vary this dynamically
     * (luck, dungeon score, ...), listen to {@link LootPoolPickEvent} and force-exclude/include
     * candidates per pick instead, or fire extra {@link com.roguesmp.loot.service.ILootService#roll}
     * calls from the caller.
     */
    private int computeTotalRolls(LootPool pool) {
        return Math.max(1, pool.getRolls());
    }

    /**
     * Picks one entry from the pool using weighted random selection.
     *
     * <p>Algorithm: compute each entry's baseline {@code weight}/{@code eligible} from its JSON
     * {@link LootCondition}s (see {@link #buildCandidates(LootPool, LootContext)}), let any
     * {@link LootPoolPickEvent} listener override those per-entry before picking, sum the
     * resulting weights of eligible entries, pick a random value in [0, totalWeight), and walk
     * entries subtracting weights until the value goes below zero.
     *
     * @return the selected entry, or null if no entry is eligible / all eligible weights are zero
     */
    private LootEntry pickWeightedEntry(LootPool pool, LootTable parentTable, LootContext context) {
        List<LootEntry> entries = pool.getEntries();
        if (entries.isEmpty()) return null;

        List<LootPoolPickEvent.Candidate> candidates = buildCandidates(pool, context);
        Bukkit.getPluginManager().callEvent(new LootPoolPickEvent(parentTable, pool, context, candidates));

        List<LootEntry> eligible = new ArrayList<>(candidates.size());
        List<Integer> weights = new ArrayList<>(candidates.size());
        int totalWeight = 0;

        for (LootPoolPickEvent.Candidate candidate : candidates) {
            if (!candidate.isEligible() || candidate.getWeight() <= 0) continue;

            eligible.add(candidate.getEntry());
            weights.add(candidate.getWeight());
            totalWeight += candidate.getWeight();
        }

        if (totalWeight <= 0) {
            RogueSmpCore.LOGGER.debug("[LootService] Pool has zero total weight after conditions/quality/listeners — skipping.");
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (int i = 0; i < eligible.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return eligible.get(i);
        }

        // Fallback (should not reach here)
        return eligible.get(eligible.size() - 1);
    }

    private List<LootPoolPickEvent.Candidate> buildCandidates(LootPool pool, LootContext context) {
        List<LootEntry> entries = pool.getEntries();
        List<LootPoolPickEvent.Candidate> candidates = new ArrayList<>(entries.size());

        for (LootEntry entry : entries) {
            boolean eligible = passesConditions(entry.getConditions(), context);
            int weight = eligible ? entry.getWeight() : 0;
            candidates.add(new LootPoolPickEvent.Candidate(entry, weight, eligible));
        }

        return candidates;
    }

    private boolean passesConditions(List<LootCondition> conditions, LootContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context)) return false;
        }
        return true;
    }

    /**
     * Runs an entry's {@link LootFunction}s in order over the items it produced, each function
     * receiving the previous one's output.
     */
    private List<ItemStack> applyFunctions(LootEntry entry, List<ItemStack> items, LootContext context) {
        if (entry.getFunctions().isEmpty()) return items;

        List<ItemStack> current = items;
        for (LootFunction function : entry.getFunctions()) {
            current = function.apply(current, context);
        }
        return current;
    }

    /**
     * Programmatic counterpart to {@link LootFunction} — lets any listener mutate a picked
     * entry's produced items directly, without needing a registered {@code LootFunction} type.
     */
    private List<ItemStack> fireEntryResultEvent(LootEntry entry, LootContext context, List<ItemStack> items) {
        LootEntryResultEvent event = new LootEntryResultEvent(entry, context, items);
        Bukkit.getPluginManager().callEvent(event);
        return event.getItems();
    }

    /**
     * Executes a single selected entry and returns the items it produces. Any entry kind not
     * explicitly handled (currently just {@code EmptyEntry}) produces nothing.
     */
    private LootRollResult executeEntry(
            LootEntry entry,
            LootContext context,
            int depth
    ) {
        if (entry instanceof ItemEntry item) return executeItemEntry(item, context);
        if (entry instanceof NestedTableEntry nested) return executeNestedTableEntry(nested, context, depth);
        return LootRollResult.empty();
    }

    /** Item ids under this prefix resolve to a plain vanilla ItemStack, bypassing BaseItem/SmpItem entirely. */
    private static final String VANILLA_ITEM_PREFIX = "minecraft:";

    /**
     * Resolves an ITEM entry — {@code "minecraft:"}-prefixed ids build a plain vanilla ItemStack
     * (see {@link #executeVanillaItemEntry}); everything else looks up a BaseItem via
     * {@link Registries#ITEM} and builds the stack via SmpItem. Either way, amount is a random
     * value in [min, max].
     */
    private LootRollResult executeItemEntry(
            ItemEntry entry,
            LootContext context
    ) {
        String itemId = entry.getItemId();
        int amount = randomAmount(entry.getMinAmount(), entry.getMaxAmount());

        if (itemId.startsWith(VANILLA_ITEM_PREFIX)) {
            return executeVanillaItemEntry(itemId, amount);
        }

        BaseItem baseItem = Registries.ITEM.get(itemId);
        if (baseItem == null) {
            RogueSmpCore.LOGGER.debug("[LootService] Unknown item_id '" + itemId + "' — skipping.");
            return LootRollResult.empty();
        }

        ItemStack stack = baseItem.generateItemStack(context.getPlayer(), amount);
        return LootRollResult.of(stack);
    }

    /**
     * Resolves a {@code "minecraft:"}-prefixed item_id to a plain vanilla {@link ItemStack} via
     * {@link Material#matchMaterial} — same match-by-name logic {@code Codec.MATERIAL} uses
     * elsewhere in the plugin. No SmpItem/BaseItem involved, so no components/lore/etc.
     */
    private LootRollResult executeVanillaItemEntry(String itemId, int amount) {
        Material material = Material.matchMaterial(itemId);
        if (material == null) {
            RogueSmpCore.LOGGER.debug("[LootService] Unknown vanilla item_id '" + itemId + "' — skipping.");
            return LootRollResult.empty();
        }

        return LootRollResult.of(new ItemStack(material, amount));
    }

    /**
     * Resolves a LOOT_TABLE entry — recurses into the nested table.
     * Results are collected directly into the parent results list via the recursive call.
     */
    private LootRollResult executeNestedTableEntry(
            NestedTableEntry entry,
            LootContext context,
            int depth
    ) {
        List<ItemStack> nestedResults = new ArrayList<>();
        rollTable(entry.getNestedTableId(), context, nestedResults, depth + 1);
        return LootRollResult.of(nestedResults);
    }

    private int randomAmount(int min, int max) {
        if (min >= max) return Math.max(1, min);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
