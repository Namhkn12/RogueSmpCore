package com.roguesmp.dungeon_spawner.repository;

import com.roguesmp.RogueSmpCore;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.roguesmp.dungeon.config.DataFolderConfig;
import com.roguesmp.dungeon_spawner.data.Spawner;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SpawnerRepository {

    private final File dataFolder;
    private final Gson gson;

    public SpawnerRepository(Plugin plugin, Gson gson) {
        this.dataFolder = new File(plugin.getDataFolder(), DataFolderConfig.getSpawnerTemplateFolder());
        this.gson = gson;

        if(!dataFolder.exists()) dataFolder.mkdirs();
    }

    public List<Spawner> loadAll() {
        List<Spawner> spawners = new ArrayList<>();
        if(!dataFolder.exists() || !dataFolder.isDirectory()){
            RogueSmpCore.LOGGER.error("Data folder not found: {}", dataFolder.getPath());
            return spawners;
        }

        File[] files = dataFolder.listFiles(((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)));
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
        File file = new File(dataFolder, DataFolderConfig.SPAWNER_TEMPLATE_FILE + spawner.getId() + DataFolderConfig.JSON_TYPE);
        try(Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(spawner, writer);
            return spawner;
        }catch (IOException e){
            RogueSmpCore.LOGGER.error("Failed to save file: {}", file.getName(), e);
            return null;
        }
    }

    public boolean delete(String id) {
        File file = new File(dataFolder, DataFolderConfig.SPAWNER_TEMPLATE_FILE + id + DataFolderConfig.JSON_TYPE);
        if(!file.exists()) return false;
        if(!file.delete()){
            RogueSmpCore.LOGGER.error("Failed to delete file: {}", file.getName());
            return false;
        }
        return true;
    }
}
