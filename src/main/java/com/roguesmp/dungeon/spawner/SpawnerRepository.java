package com.roguesmp.dungeon.spawner;

import com.roguesmp.RogueSmpCore;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SpawnerRepository {

    private static final String FOLDER = "dungeon_v2/template/spawner_template";
    private static final String FILE_PREFIX = "spawner";

    private final File dataFolder;
    private final Gson gson;

    public SpawnerRepository(Plugin plugin, Gson gson) {
        this.dataFolder = new File(plugin.getDataFolder(), FOLDER);
        this.gson = gson;

        if(!dataFolder.exists()) dataFolder.mkdirs();
    }

    public List<Spawner> loadAll() {
        List<Spawner> spawners = new ArrayList<>();
        if(!dataFolder.exists() || !dataFolder.isDirectory()){
            RogueSmpCore.LOGGER.error("Data folder not found: {}", dataFolder.getPath());
            return spawners;
        }

        File[] files = dataFolder.listFiles(((dir, name) -> name.endsWith(".json")));
        if(files == null) return spawners;

        for(File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Spawner spawner = gson.fromJson(reader, Spawner.class);
                if(spawner != null && spawner.getId() != null){
                    spawners.add(spawner);
                }else {
                    RogueSmpCore.LOGGER.debug("Data is null, please check: {}", file.getAbsolutePath());
                }
            } catch (JsonParseException | IOException e){
                RogueSmpCore.LOGGER.error("Invalid data in file: {}", file.getAbsolutePath());
            }
        }
        return spawners;
    }

    public Spawner save(Spawner spawner) {
        File file = new File(dataFolder, FILE_PREFIX + spawner.getId() + ".json");
        try(Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(spawner, writer);
            return spawner;
        }catch (IOException e){
            RogueSmpCore.LOGGER.error("Failed to save file: {}", file.getName(), e);
            return null;
        }
    }

    public boolean delete(String id) {
        File file = new File(dataFolder, FILE_PREFIX + id + ".json");
        if(!file.exists()) return false;
        if(!file.delete()){
            RogueSmpCore.LOGGER.error("Failed to delete file: {}", file.getName());
            return false;
        }
        return true;
    }
}
