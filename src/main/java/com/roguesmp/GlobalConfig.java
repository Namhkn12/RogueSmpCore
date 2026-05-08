package com.roguesmp;

import com.roguesmp.utils.Utils;

import java.io.*;

public class GlobalConfig {

    private final String mineskinApiKey;

    public GlobalConfig(String mineskinApiKey) {
        this.mineskinApiKey = mineskinApiKey;
    }

    public GlobalConfig() {
        mineskinApiKey = "mineSkinApiKey";
    }

    public static GlobalConfig loadGlobalConfig(RogueSmpCore plugin) {
        File file = new File(plugin.getDataFolder(), "global_config.json");

        // 1. Create default file if it doesn't exist
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                // Serialize a fresh instance of GlobalConfig to create the template
                Utils.GSON.toJson(new GlobalConfig(), writer);
                RogueSmpCore.LOGGER.info("Created default global_config.json");
            } catch (IOException e) {
                RogueSmpCore.LOGGER.error("Failed to create default config!", e);
            }
        }

        // 2. Load the file using Gson's default deserializer
        try (Reader reader = new FileReader(file)) {
            GlobalConfig config = Utils.GSON.fromJson(reader, GlobalConfig.class);

            // If file is empty or invalid, return a fresh default instance instead of null
            if (config == null) {
                return new GlobalConfig();
            }

            RogueSmpCore.LOGGER.info("Global configuration loaded.");
            return config;
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load global_config.json, using defaults.", e);
            return new GlobalConfig();
        }
    }

    public String getMineskinApiKey() {
        return mineskinApiKey;
    }
}
