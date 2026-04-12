package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
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

    private PlayerManager(RogueSmpCore plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        players = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += PERIOD;
                if (ticks >= 20) ticks = 0;

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

    public void loadPlayer(UUID uuid) {
        SmpPlayer smpPlayer = new SmpPlayer(uuid);
        players.put(uuid, smpPlayer);
        PlayerData playerData = dataManager.getData(uuid);
        if (playerData == null) return;
        smpPlayer.loadData(playerData);
    }

    public void unloadPlayer(UUID uuid) {
        SmpPlayer smpPlayer = players.get(uuid);
        if (smpPlayer == null) return;
        PlayerData playerData = dataManager.removeCachedData(uuid);
        players.remove(uuid);
        Utils.runAsync(() -> dataManager.savePlayerData(playerData));
    }

    public void onDisable() {
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            SmpPlayer smpPlayer = players.get(player.getUniqueId());
            if (smpPlayer == null) return;
            PlayerData playerData = dataManager.removeCachedData(smpPlayer.getUuid());
            dataManager.savePlayerData(playerData);
        });
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
