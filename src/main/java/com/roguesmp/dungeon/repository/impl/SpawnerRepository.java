package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.dto.DataResult;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon.repository.ISpawnerRepository;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SpawnerRepository implements ISpawnerRepository {
    private final File spawnerFolder;
    private final Gson gson;

    public SpawnerRepository(Plugin plugin, Gson gson) {
        this.spawnerFolder = new File(plugin.getDataFolder(), DataConfig.getSpawnerTemplateFolder());
        this.gson = gson;

        if (!spawnerFolder.exists()) {
            spawnerFolder.mkdirs();
        }
    }

    @Override
    public DataResult<Map<String, Spawner>> loadAll() {
        Map<String, Spawner> spawnerMap = new HashMap<>();
        List<String> errors = new ArrayList<>();
        if (!spawnerFolder.exists() || !spawnerFolder.isDirectory()) {
            throw new DataLoadException(spawnerFolder.getName(), null);
        }

        File[] files = spawnerFolder.listFiles((dir, name) -> name.endsWith(DataConfig.JSON_TYPE));
        if (files == null) return new DataResult<>(spawnerMap, errors);

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Spawner spawner = gson.fromJson(reader, Spawner.class);
                if (spawner != null && spawner.getId() != null) {
                    spawnerMap.put(spawner.getId(), spawner);
                } else {
                    errors.add("Invalid data in file: " + file.getName());
                }
            } catch (JsonParseException | IOException e) {
                errors.add(file.getName() + " - " + e.getMessage());
            }
        }
        return new DataResult<>(spawnerMap, errors);
    }

    @Override
    public Spawner save(Spawner spawner) {
        File file = new File(spawnerFolder, DataConfig.SPAWNER_TEMPLATE_FILE + spawner.getId() + DataConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(spawner, writer);
            return spawner;
        }catch (IOException e){
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public Boolean delete(String id) {
        File file = new File(spawnerFolder, id + DataConfig.JSON_TYPE);

        if (!file.exists()) return false;

        if (!file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
        return true;
    }
}
