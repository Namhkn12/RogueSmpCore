package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.repository.IDungeonRepository;

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
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public boolean delete(String id) {
        return new File(dungeonFolder, DataConfig.DUNGEON_TEMPLATE_FILE + id + DataConfig.JSON_TYPE).delete();
    }
}
