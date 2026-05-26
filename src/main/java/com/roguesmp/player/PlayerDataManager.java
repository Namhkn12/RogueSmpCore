package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle saving, loading
 */
public class PlayerDataManager {
    public static final String FOLDER = "player_data";

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerDataManager() {

    }

    public void cacheData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
    }

    public PlayerData removeCachedData(UUID uuid) {
        return playerDataCache.remove(uuid);
    }

    public @Blocking void savePlayerData(PlayerData playerData) {
        if (playerData == null) return;

        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, playerData.getUuid().toString() + ".json");

        try (Writer writer = new FileWriter(file)) {
            Utils.GSON.toJson(playerData, writer);
            RogueSmpCore.LOGGER.info("Player data saved (uuid: {})", playerData.getUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public @Blocking PlayerData loadPlayerData(UUID uuid) {
        RogueSmpCore.LOGGER.info("Loading player data (uuid: {})", uuid);
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
            RogueSmpCore.LOGGER.info("Player data file not found, creating default");
        }

        File file = new File(folder, uuid.toString() + ".json");

        if (!file.exists()) {
            return createDefault(uuid);
        }

        try (Reader reader = new FileReader(file)) {
            PlayerData data = Utils.GSON.fromJson(reader, PlayerData.class);

            if (data == null) {
                RogueSmpCore.LOGGER.info("Player data file is empty, creating default data");
                return createDefault(uuid);
            }
            RogueSmpCore.LOGGER.info("Loaded player data (uuid: {})", uuid);
            return data;

        } catch (IOException e) {
            e.printStackTrace();
            return createDefault(uuid);
        }
    }

    public @Nullable PlayerData getData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    private static PlayerData createDefault(UUID uuid) {
        return new PlayerData(uuid);
    }
}
