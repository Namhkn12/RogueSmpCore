package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static PlayerManager INSTANCE = null;

    private final RogueSmpCore plugin;
    private final Map<UUID, SmpPlayer> players;

    private PlayerManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        players = new HashMap<>();
    }

    public @Nullable SmpPlayer getSmpPlayer(UUID uuid) {
        return players.getOrDefault(uuid, null);
    }

    public void addPlayer(UUID uuid) {
        players.put(uuid, new SmpPlayer(uuid));
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(PlayerManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public static void init(RogueSmpCore core) {
        INSTANCE = new PlayerManager(core);
    }
}
