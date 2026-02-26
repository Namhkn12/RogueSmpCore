package com.roguesmp.dungeon.instance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import com.google.gson.reflect.TypeToken;

public class DungeonInstanceManager {

    private final Map<String, DungeonInstance> dgInstances = new HashMap<>();
    private final File file;
    private final Gson gson;
    private final RogueSmpCore plugin;

    public DungeonInstanceManager(RogueSmpCore plugin) {
        this.plugin = plugin;

        File folder = new File(plugin.getDataFolder(), "dungeons");
        if (!folder.exists()) folder.mkdirs();

        this.file = new File(folder, "dungeon_instances.json");

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public void load() {
        if (!file.exists()) {
            plugin.getLogger().info("No dungeon instance file found.");
            return;
        }

        try (Reader reader = new FileReader(file)) {

            Type type = new TypeToken<Map<String, DungeonInstance>>() {}.getType();
            Map<String, DungeonInstance> loaded = gson.fromJson(reader, type);

            if (loaded != null) {
                dgInstances.clear();
                dgInstances.putAll(loaded);
            }

            plugin.getLogger().info("Loaded " + dgInstances.size() + " dungeon instances.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(dgInstances, writer);
            plugin.getLogger().info("Saved " + dgInstances.size() + " dungeon instances.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DungeonInstance createDungeonInstance(
            String partyId,
            String dungeonId,
            String regionId,
            List<String> roomIds
    ) {

        UUID uuid = UUID.randomUUID();

        DungeonInstance instance = new DungeonInstance(
                uuid,
                System.currentTimeMillis(),
                partyId,
                dungeonId,
                regionId,
                roomIds,
                true
        );

        dgInstances.put(uuid.toString(), instance);

        return instance;
    }

    public void deleteDungeonInstance(String instanceId) {
        dgInstances.remove(instanceId);
    }

    public DungeonInstance getDungeonInstance(String instanceId) {
        return dgInstances.get(instanceId);
    }

    public Collection<DungeonInstance> getAllInstances() {
        return dgInstances.values();
    }

    public boolean exists(String instanceId) {
        return dgInstances.containsKey(instanceId);
    }
}