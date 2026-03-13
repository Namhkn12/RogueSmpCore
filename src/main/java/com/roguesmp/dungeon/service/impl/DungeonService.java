package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Room;
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

    private void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Dungeon name must not be empty.");
        if (name.length() > 64)
            throw new IllegalArgumentException("Dungeon name must not exceed 64 characters.");
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Dungeon description must not be empty.");
        if (description.length() > 256)
            throw new IllegalArgumentException("Dungeon description must not exceed 256 characters.");
    }
}