package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.definition.loot.LootTable;

import java.util.Map;

public interface ILootTableRepository {

    Map<String, LootTable> loadAll();

    LootTable loadById(String id);
}
