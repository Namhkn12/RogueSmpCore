package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle runtime player instances, ticking them
 */
public class PlayerManager {
    private static PlayerManager INSTANCE = null;

    public static final int PERIOD = 5;

    private final RogueSmpCore plugin;
    private final PlayerDataManager dataManager;
    private final Map<UUID, SmpPlayer> players;

    private PlayerManager(RogueSmpCore plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        players = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += PERIOD;
                boolean twoHz = ticks % 10 == 0;
                boolean oneHz = ticks % 20 == 0;
                if (ticks >= 20) ticks = 0;

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
        Utils.runAsync(() -> {
            PlayerData playerData = dataManager.loadPlayerData(uuid);
            Utils.runLater(() -> {
                dataManager.registerData(playerData);
                SmpPlayer smpPlayer = new SmpPlayer(uuid);
                players.put(uuid, smpPlayer);
                smpPlayer.loadData();
            });
        });
    }

    public void unloadPlayer(UUID uuid) {
        SmpPlayer smpPlayer = players.get(uuid);
        if (smpPlayer == null) return;
        smpPlayer.saveData();
        Utils.runAsync(() -> {
            dataManager.savePlayerData(uuid);
            Utils.runLater(() -> dataManager.unregisterData(uuid));
        });
        players.remove(uuid);
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(PlayerManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public static void init(RogueSmpCore core, PlayerDataManager dataManager) {
        INSTANCE = new PlayerManager(core, dataManager);
    }
}
