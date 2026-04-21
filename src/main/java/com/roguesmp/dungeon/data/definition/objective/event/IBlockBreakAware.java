package com.roguesmp.dungeon.data.definition.objective.event;

import org.bukkit.Material;

/**
 * Receives block-break signals from the dungeon runtime.
 */
public interface IBlockBreakAware {
    void onBlockBreak(Material type);
}
