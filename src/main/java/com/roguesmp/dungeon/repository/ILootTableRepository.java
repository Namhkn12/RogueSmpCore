package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.LootTable;
import com.roguesmp.dungeon.dto.DataResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ILootTableRepository {

    /**
     * Loads all loot tables found in the data folder.
     * Returns a {@link DataResult} containing the loaded tables and any non-fatal errors.
     * Throws {@link com.roguesmp.dungeon.exception.impl.data.DataLoadException} if the folder is inaccessible.
     */
    @NotNull DataResult<Map<String, LootTable>> loadAll();

    /**
     * Loads a single loot table by ID.
     * e.g. {@code "rogue:dungeons/dungeon_a_reward"}
     *
     * @return the loaded {@link LootTable}, or null if not found or parse failed
     */
    @Nullable LootTable loadById(@NotNull String id);
}
