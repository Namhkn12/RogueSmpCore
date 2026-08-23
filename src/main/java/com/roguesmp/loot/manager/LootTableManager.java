package com.roguesmp.loot.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Registries;

import java.util.Map;

/**
 * Thin read-facing wrapper around {@link Registries#LOOT_TABLE}. The initial load happens
 * automatically as part of {@link Registries#loadAllData} (like every other data-driven
 * registry) - {@link #load()} here exists purely for an explicit manual reload (e.g.
 * {@code /template reload loottable}).
 */
public class LootTableManager {

    public void load() {
        Registries.LOOT_TABLE.clear();
        Registries.LOOT_TABLE.loadFrom(RogueSmpCore.getInstance());
    }

    public LootTable getTable(String id) {
        return Registries.LOOT_TABLE.get(id);
    }

    public boolean exists(String id) {
        return Registries.LOOT_TABLE.get(id) != null;
    }

    public Map<String, LootTable> getAllTables() {
        return Registries.LOOT_TABLE.getAll();
    }

    public int getCacheSize() {
        return Registries.LOOT_TABLE.getAll().size();
    }
}
