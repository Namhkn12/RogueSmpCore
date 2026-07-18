package com.roguesmp.loot.repository;

import com.roguesmp.loot.LootTable;

import java.util.Map;

public interface ILootTableRepository {

    Map<String, LootTable> loadAll();

    LootTable loadById(String id);
}
