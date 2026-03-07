package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle saving, loading
 */
public class PlayerDataManager {
    public static final String FOLDER = "player_data";

    private static PlayerDataManager INSTANCE;

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    private PlayerDataManager() {

    }

    public void saveAllPlayers() {
        RogueSmpCore.LOGGER.info("Saving data for {} players...", playerDataCache.size());
        playerDataCache.forEach((uuid, data) -> savePlayerData(uuid));
    }

    public void registerData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
    }

    public void unregisterData(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    public void savePlayerData(UUID uuid) {
        RogueSmpCore.LOGGER.info("Saving player data (uuid: {})", uuid);
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;

        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, data.getUuid().toString() + ".json");

        try (Writer writer = new FileWriter(file)) {
            Utils.GSON.toJson(data, writer);
            RogueSmpCore.LOGGER.info("Player data saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
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

    public PlayerData getData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    private static PlayerData createDefault(UUID uuid) {
        return new PlayerData(uuid);
    }

    public static void init() {
        INSTANCE = new PlayerDataManager();
    }

    public static PlayerDataManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("PlayerDataManager is null");
        }
        return INSTANCE;
    }
}
