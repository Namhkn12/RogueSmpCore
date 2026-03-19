package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constraint.FolderConfig;
import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.repository.ISpawnerRepository;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SpawnerRepository implements ISpawnerRepository {
    private final File spawnerFolder;
    private final Gson gson;

    public SpawnerRepository(Plugin plugin, Gson gson) {
        this.spawnerFolder = new File(plugin.getDataFolder(), FolderConfig.getSpawnerTemplateFolder());
        this.gson = gson;
    }

    @Override
    public Map<String, Spawner> loadAll() {
        Map<String, Spawner> spawnerMap = new HashMap<>();
        File[] files =  spawnerFolder.listFiles(((dir, name) -> name.endsWith(FolderConfig.JSON_TYPE)));
        if(files == null) return spawnerMap;

        for(File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Spawner spawner = gson.fromJson(reader, Spawner.class);
                if(spawner != null && spawner.getId() != null){
                    spawnerMap.put(spawner.getId(), spawner);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return spawnerMap;
    }

    @Override
    public Spawner save(Spawner spawner) {
        File file = new File(spawnerFolder, spawner.getId() + FolderConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(spawner, writer);
            return spawner;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean delete(String id) {
        return new File(spawnerFolder, id + FolderConfig.JSON_TYPE).delete();
    }
}
