package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.definition.loot.LootTable;

import java.util.Map;

public interface ILootTableRepository {

    Map<String, LootTable> loadAll();

    LootTable loadById(String id);
}
