package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.ultis.TimeId;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DungeonManager {

    private final String prefix = "dungeon";
    private final String folder = "dungeons";
    private static DungeonManager INSTANCE = null;

    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final File dungeonFolder;
    private final Gson gson;
    private final RogueSmpCore plugin;

    private DungeonManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.dungeonFolder = new File(plugin.getDataFolder(), folder);

        if (!dungeonFolder.exists()) {
            dungeonFolder.mkdirs();
        }

        load();
    }

    public static void init(RogueSmpCore plugin) {
        if (INSTANCE == null) {
            INSTANCE = new DungeonManager(plugin);
        }
    }

    public static DungeonManager getInstance() {
        return INSTANCE;
    }

    public void registerCommand(){
        new DungeonCommand().register();
    }

    public void load() {
        dungeons.clear();

        File[] files = dungeonFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {

                Dungeon dungeon = gson.fromJson(reader, Dungeon.class);

                if (dungeon != null && dungeon.getDgId() != null) {
                    dungeons.put(dungeon.getDgId(), dungeon);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load dungeon: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + dungeons.size() + " dungeons.");
    }

    public Dungeon create(String name, String description, List<String> rooms) {

        String dgId = prefix + '_' + TimeId.generateTimeId();
        Dungeon dungeon = new Dungeon(dgId, name, description, rooms);

        dungeons.put(dgId, dungeon);
        save(dungeon);

        return dungeon;
    }

    public void save(Dungeon dungeon) {

        if (dungeon == null || dungeon.getDgId() == null) return;

        dungeons.put(dungeon.getDgId(), dungeon); // override

        File file = new File(dungeonFolder, dungeon.getDgId() + ".json");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {

            gson.toJson(dungeon, writer);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save dungeon: " + dungeon.getDgId());
            e.printStackTrace();
        }
    }

    public void delete(String dgId) {

        Dungeon removed = dungeons.remove(dgId);
        if (removed == null) return;

        File file = new File(dungeonFolder, dgId + ".json");

        if (file.exists()) {
            file.delete();
        }
    }


    public Dungeon getById(String dgId) {
        return dungeons.get(dgId);
    }

    public Collection<Dungeon> getAll() {
        return dungeons.values();
    }

    public boolean exists(String dgId) {
        return dungeons.containsKey(dgId);
    }
}