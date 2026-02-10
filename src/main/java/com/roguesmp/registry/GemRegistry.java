package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.gem.GemData;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class GemRegistry {
    private static GemRegistry INSTANCE = null;

    public static final String FOLDER_NAME = "gems";

    private final RogueSmpCore plugin;

    private final Map<String, GemData> dataMap = new HashMap<>();

    private GemRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;

        dataMap.put("test_gem", new GemData("test_gem", "fallback_item",
                Map.of(EquipSlot.MAINHAND, Map.of(Attributes.PHYSICAL_DAMAGE_BASE, 10d))));
    }

    public @Nullable GemData getGemData(String id) {
        return dataMap.get(id);
    }

    private File getDataFolder() {
        return new File(plugin.getDataFolder(), FOLDER_NAME);
    }

    public void saveToFile(boolean override) {
        File file = getDataFolder();

        if (!file.exists() && !file.mkdirs()) {
            plugin.getLogger().severe("Failed to create " + FOLDER_NAME +  " directory");
            return;
        }

        plugin.getLogger().info("Saving gem data registry...");

        for (Map.Entry<String, GemData> entry : dataMap.entrySet()) {
            String id = entry.getKey();
            GemData gemData = entry.getValue();

            File child = new File(file, id + ".json");
            if (child.exists() && !override) {
                return;
            }

            try (Writer writer = new FileWriter(child)) {
                Utils.GSON.toJson(gemData, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save gem data: " + id);
            }
        }
    }

    public void loadFromFile() {
        File dataDirs = new File(plugin.getDataFolder(), FOLDER_NAME);

        if (!dataDirs.exists() || !dataDirs.isDirectory()) {
            plugin.getLogger().info("No " + FOLDER_NAME + " folder found, starting with empty registry");
            return;
        }

        File[] files = dataDirs.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info(FOLDER_NAME + " folder is empty");
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                GemData gemData = Utils.GSON.fromJson(reader, GemData.class);

                if (gemData == null || gemData.getId() == null) {
                    plugin.getLogger().warning("Invalid gem data file: " + file.getName());
                    continue;
                }

                dataMap.put(gemData.getId(), gemData);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load gem data file: " + file.getName());
            }
        }

        plugin.getLogger().info("Loaded gem registry (" + dataMap.size() + " entries)");
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new GemRegistry(plugin);
    }

    public static GemRegistry getInstance() {
        if (INSTANCE == null) {
            throw new NullPointerException("Gem registry is null");
        }
        return INSTANCE;
    }


}
