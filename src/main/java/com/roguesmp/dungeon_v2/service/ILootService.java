package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.definition.loot.LootContext;
import org.bukkit.inventory.ItemStack;
import java.util.List;

/**
 * Contract for rolling loot tables and producing item results.
 *
 * <p>This is the main entry point for anything that needs items from a loot table:
 * reward chests, entity drops, quest rewards, etc.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve loot table from cache (via LootTableManager)</li>
 *   <li>Apply LootContext rules to compute bonus rolls</li>
 *   <li>Perform weighted random entry selection per pool</li>
 *   <li>Recursively resolve nested LOOT_TABLE entries</li>
 *   <li>Build final ItemStack list via ItemRegistry + SmpItem</li>
 * </ul>
 *
 * <p>This service does NOT handle spawning/dropping items in the world —
 * that is the responsibility of the caller (Actor/Listener layer).
 *
 * <p>Fits the model:
 * <pre>
 * Actor (ChestListener / EntityDeathListener)
 *   └── LootService.roll(tableId, context)
 *         ├── LootTableManager.getTable(tableId)   [Manager/Cache]
 *         ├── WeightedRandom + BonusRolls logic
 *         └── ItemRegistry.getBaseItem(itemId)      [Registry]
 *               └── BaseItem.generateItemStack(player, amount)
 * </pre>
 */
public interface ILootService {

    /**
     * Rolls a loot table and returns the resulting items.
     *
     * @param lootTableId the loot table ID, e.g. {@code "rogue:dungeons/dungeon_a_reward"}
     * @param context     runtime context carrying player info and active LootRules
     * @return list of generated ItemStacks. Never null, may be empty.
     */
    List<ItemStack> roll(String lootTableId, LootContext context);

    /**
     * Convenience overload — rolls without any LootContext (no player, no bonus rules).
     * Useful for simple drops that don't benefit from luck/looting.
     *
     * @param lootTableId the loot table ID
     * @return list of generated ItemStacks. Never null, may be empty.
     */
    List<ItemStack> roll(String lootTableId);

    /**
     * Returns true if the loot table exists and is loaded in cache.
     * Use this to validate IDs before rolling (e.g. on chest setup).
     *
     * @param lootTableId the loot table ID to check
     */
    boolean exists(String lootTableId);
}