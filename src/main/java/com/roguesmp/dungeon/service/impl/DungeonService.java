package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.service.IDungeonService;

import java.util.List;
import java.util.Optional;

public class DungeonService implements IDungeonService {

    private final DungeonManager dungeonManager;

    public DungeonService(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
    }

    @Override
    public Dungeon createDungeon(String name) {
        return dungeonManager.create(name);
    }

    @Override
    public Optional<Dungeon> getDungeonById(String dgId) {
        if (dgId == null || dgId.isBlank()) return Optional.empty();

        return dungeonManager.getById(dgId);
    }

    @Override
    public List<String> getDungeonIdList() {
        return dungeonManager.getDungeonIdList();
    }

    @Override
    public boolean deleteDungeon(String dgId) {
        if (dgId == null || dgId.isBlank()) return false;
        if (!dungeonManager.exists(dgId)) return false;

        return dungeonManager.delete(dgId);
    }
}