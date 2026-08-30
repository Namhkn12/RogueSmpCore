package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    private PlayerManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.dataManager = new PlayerDataManager();
        players = new HashMap<>();

        new BukkitRunnable() {

            @Override
            public void run() {
                for (SmpPlayer player : players.values()) {
                    player.tick(PERIOD);
                }
            }
        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    public @Nullable SmpPlayer getSmpPlayer(UUID uuid) {
        return players.getOrDefault(uuid, null);
    }

    public @Nullable SmpPlayer getSmpPlayer(Player player) {
        return getSmpPlayer(player.getUniqueId());
    }

    public void loadAndTrackPlayer(UUID uuid) {
        SmpPlayer smpPlayer = new SmpPlayer(uuid);
        players.put(uuid, smpPlayer);
        PlayerData playerData = dataManager.getData(uuid);
        if (playerData == null) {
            RogueSmpCore.LOGGER.warn("PlayerData for uuid {} is not loaded!", uuid);
            return;
        }
        smpPlayer.loadData(playerData);
    }

    public void untrackPlayer(UUID uuid) {
        players.remove(uuid);
    }

    public void onDisable() {
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            SmpPlayer smpPlayer = players.get(player.getUniqueId());
            if (smpPlayer == null) return;
            PlayerData playerData = dataManager.removeCachedData(smpPlayer.getUuid());
            dataManager.savePlayerData(playerData);
        });
        dataManager.close();
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
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
