package com.roguesmp.player;

import com.roguesmp.player.data.AbilityLevelPair;

import java.util.List;
import java.util.UUID;

/**
 * A class that hold player-related data
 */
public class PlayerData {
    private final UUID uuid;
    private final String lastKnownName;
    private final List<AbilityLevelPair> unlockedAnilities;

    public PlayerData(UUID uuid, String lastKnownName, List<AbilityLevelPair> unlockedAnilities) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.unlockedAnilities = unlockedAnilities;
    }
}
