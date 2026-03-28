package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon.repository.IDungeonRepository;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DungeonRepository implements IDungeonRepository {

    private final File dungeonFolder;
    private Gson gson;

    public DungeonRepository(Gson gson) {
        this.gson = gson;
        this.dungeonFolder = new File(RogueSmpCore.getInstance().getDataFolder(), DataConfig.getDungeonTemplateFolder());

        if (!dungeonFolder.exists()) {
            dungeonFolder.mkdirs();
        }
    }

    @Override
    public List<Dungeon> loadAll() {
        List<Dungeon> dungeons = new ArrayList<>();
        File[] files = dungeonFolder.listFiles(((dir, name) -> name.endsWith(DataConfig.JSON_TYPE)));
        if(files == null) return dungeons;

        for (File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Dungeon dungeon = gson.fromJson(reader, Dungeon.class);
                if(dungeon != null && dungeon.getDgId() != null){
                    dungeons.add(dungeon);
                }
            }catch (Exception e){
                Log4Craft.fire("Failed to load dungeon template: " + file.getName(), e);
            }
        }
        return dungeons;
    }

    @Override
    public void save(Dungeon dungeon) {
        File file = new File(dungeonFolder, dungeon.getDgId() + DataConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(dungeon, writer);
        }catch (Exception e){
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        File file = new File(dungeonFolder, id + DataConfig.JSON_TYPE);
        if (!file.exists()) return false;
        if (!file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
        return true;
    }
}
