package com.roguesmp;

import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;

import java.io.*;

public class GlobalConfig {

    private final String mineskinApiKey;
    private final String resourcePackUrl;
    private final String resourcePackHash;
    private long lastDailyReset; // Removed final to allow updates

    public GlobalConfig(String mineskinApiKey, String resourcePackUrl, String resourcePackHash, long lastDailyReset) {
        this.mineskinApiKey = mineskinApiKey;
        this.resourcePackUrl = resourcePackUrl;
        this.resourcePackHash = resourcePackHash;
        this.lastDailyReset = lastDailyReset;
    }

    public GlobalConfig() {
        this.lastDailyReset = 0;
        this.mineskinApiKey = "mineSkinApiKey";
        this.resourcePackUrl = "https://cdn.minepack.fr/pack/cd5754e4-701d-4037-a815-935b1a2f1290";
        this.resourcePackHash = "c6d5dcd8b725ca5793471bb71fb6639e27d85540";
    }

    public static @Blocking GlobalConfig loadGlobalConfig(RogueSmpCore plugin) {
        File file = getConfigFile(plugin);

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                Utils.GSON.toJson(new GlobalConfig(), writer);
                RogueSmpCore.LOGGER.info("Created default global_config.json");
            } catch (IOException e) {
                RogueSmpCore.LOGGER.error("Failed to create default config!", e);
            }
        }

        try (Reader reader = new FileReader(file)) {
            GlobalConfig config = Utils.GSON.fromJson(reader, GlobalConfig.class);
            if (config == null) return new GlobalConfig();

            RogueSmpCore.LOGGER.info("Global configuration loaded.");
            return config;
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load global_config.json, using defaults.", e);
            return new GlobalConfig();
        }
    }

    // Helper method to get the config File instance
    public static File getConfigFile(RogueSmpCore plugin) {
        return new File(plugin.getDataFolder(), "global_config.json");
    }

    // Save configuration back to disk when values update
    public @Blocking void save(RogueSmpCore plugin) {
        File file = getConfigFile(plugin);
        try (FileWriter writer = new FileWriter(file)) {
            Utils.GSON.toJson(this, writer);
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to save global_config.json!", e);
        }
    }

    public String getMineskinApiKey() {
        return mineskinApiKey;
    }

    public long getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public String getResourcePackHash() {
        return resourcePackHash;
    }

    public String getResourcePackUrl() {
        return resourcePackUrl;
    }
}
