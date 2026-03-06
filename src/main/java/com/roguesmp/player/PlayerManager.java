package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static PlayerManager INSTANCE = null;

    public static final String FOLDER = "player_data";

    public static final int PERIOD = 5;

    private final RogueSmpCore plugin;
    private final Map<UUID, SmpPlayer> players;

    private PlayerManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        players = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += PERIOD;
                boolean twoHz = ticks % 10 == 0;
                boolean oneHz = ticks % 20 == 0;
                ticks = 0;

                for (SmpPlayer player : players.values()) {
                    player.tick(twoHz, oneHz);
                }
            }
        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    public @Nullable SmpPlayer getSmpPlayer(UUID uuid) {
        return players.getOrDefault(uuid, null);
    }

    public void loadPlayer(UUID uuid) {
        players.put(uuid, new SmpPlayer(uuid));

        Utils.runAsync(() -> {

        });
    }

    public void unloadPlayer(UUID uuid) {
        players.remove(uuid);
    }

    private void loadPlayerAbilities() {

    }

    private void savePlayerAbilities(UUID playerId) {
        File folder = new File(plugin.getDataFolder(), FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File playerFile = new File(folder, playerId.toString() + ".json");

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
