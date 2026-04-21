package com.roguesmp.dungeon.data.definition.objective.event;

/**
 * Receives item-collection signals from the dungeon runtime.
 */
public interface IItemCollectAware {
    void onItemCollected(String itemId, int amount);
}
