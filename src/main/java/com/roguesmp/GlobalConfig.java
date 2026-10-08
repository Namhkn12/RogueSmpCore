package com.roguesmp;

import com.google.gson.JsonElement;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;

import java.io.*;

public class GlobalConfig {

    private static final String DEFAULT_MINESKIN_API_KEY = "mineSkinApiKey";
    private static final String DEFAULT_RESOURCE_PACK_URL = "https://cdn.minepack.fr/pack/cd5754e4-701d-4037-a815-935b1a2f1290";
    private static final String DEFAULT_RESOURCE_PACK_HASH = "c6d5dcd8b725ca5793471bb71fb6639e27d85540";

    public static final Codec<GlobalConfig> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("mineskinApiKey", DEFAULT_MINESKIN_API_KEY).forGetter(GlobalConfig::getMineskinApiKey),
            Codec.STRING.optionalFieldOf("resourcePackUrl", DEFAULT_RESOURCE_PACK_URL).forGetter(GlobalConfig::getResourcePackUrl),
            Codec.STRING.optionalFieldOf("resourcePackHash", DEFAULT_RESOURCE_PACK_HASH).forGetter(GlobalConfig::getResourcePackHash),
            Codec.LONG.optionalFieldOf("lastDailyReset", 0L).forGetter(GlobalConfig::getLastDailyReset),
            GlobalConfig::new
    );

    private final String mineskinApiKey;
    private final String resourcePackUrl;
    private final String resourcePackHash;
    private long lastDailyReset;

    public GlobalConfig(String mineskinApiKey, String resourcePackUrl, String resourcePackHash, long lastDailyReset) {
        this.mineskinApiKey = mineskinApiKey;
        this.resourcePackUrl = resourcePackUrl;
        this.resourcePackHash = resourcePackHash;
        this.lastDailyReset = lastDailyReset;
    }

    private static GlobalConfig defaults() {
        return CODEC.decode(JsonOps.INSTANCE.emptyMap(), JsonOps.INSTANCE).result();
    }

    public static @Blocking GlobalConfig loadGlobalConfig(RogueSmpCore plugin) {
        File file = getConfigFile(plugin);

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            defaults().save(plugin);
            RogueSmpCore.LOGGER.info("Created default global_config.json");
        }

        try (Reader reader = new FileReader(file)) {
            JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
            if (json == null) return defaults();

            DataResult<GlobalConfig> result = CODEC.decode(json, JsonOps.INSTANCE);
            if (!result.isSuccess()) {
                RogueSmpCore.LOGGER.error("Failed to decode global_config.json, using defaults: {}", result.error());
                return defaults();
            }

            RogueSmpCore.LOGGER.info("Global configuration loaded.");
            return result.result();
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load global_config.json, using defaults.", e);
            return defaults();
        }
    }

    public static File getConfigFile(RogueSmpCore plugin) {
        return new File(plugin.getDataFolder(), "global_config.json");
    }

    public @Blocking void save(RogueSmpCore plugin) {
        DataResult<JsonElement> encoded = CODEC.encode(this, JsonOps.INSTANCE);
        if (!encoded.isSuccess()) {
            RogueSmpCore.LOGGER.error("Failed to encode global_config.json: {}", encoded.error());
            return;
        }

        try (FileWriter writer = new FileWriter(getConfigFile(plugin))) {
            Utils.GSON.toJson(encoded.result(), writer);
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
