package com.roguesmp.dungeon.constant;

/**
 * Defines the type of a single entry inside a loot pool.
 *
 * - ITEM       : Drop a specific item by item_id (resolved via ItemRegistry)
 * - LOOT_TABLE : Delegate rolling to a nested loot table (recursive)
 * - EMPTY      : Drop nothing — used to add a weighted "no drop" chance
 */
public enum LootEntryType {
    ITEM,
    LOOT_TABLE,
    EMPTY
}