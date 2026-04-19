package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;
import com.roguesmp.dungeon_v2.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon_v2.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon_v2.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon_v2.repository.ISpawnerRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SpawnerRepository implements ISpawnerRepository {

    private final File dataFolder;
    private final Gson gson;
    private final Log4Craft_ logger;

    public SpawnerRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.dataFolder = new File(plugin.getDataFolder(), DataFolderConfig.getSpawnerTemplateFolder());
        this.gson = gson;
        this.logger = logger;

        if(!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public List<Spawner> loadAll() {
        List<Spawner> spawners = new ArrayList<>();
        if(!dataFolder.exists() || !dataFolder.isDirectory()){
            throw new DataLoadException(dataFolder.getName(), null);
        }

        File[] files = dataFolder.listFiles(((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)));
        if(files == null) return spawners;

        for(File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Spawner spawner = gson.fromJson(reader, Spawner.class);
                if(spawner != null && spawner.getId() != null){
                    spawners.add(spawner);
                }else {
                    logger.debug(this.getClass(), "Data is null, please check: " + file.getAbsolutePath());
                }
            } catch (JsonParseException | IOException e){
                logger.error(this.getClass(),   "Invalid data in file: " + file.getAbsolutePath());
            }
        }
        return spawners;
    }

    @Override
    public Spawner save(Spawner spawner) {
        File file = new File(dataFolder, DataFolderConfig.SPAWNER_TEMPLATE_FILE + spawner.getId() + DataFolderConfig.JSON_TYPE);
        try(Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(spawner, writer);
            return spawner;
        }catch (IOException e){
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        File file = new File(dataFolder, DataFolderConfig.SPAWNER_TEMPLATE_FILE + id + DataFolderConfig.JSON_TYPE);
        if(!file.exists()) return false;
        if(!file.delete()){
            throw new DataDeleteException(file.getName(), null);
        }
        return true;
    }
}
