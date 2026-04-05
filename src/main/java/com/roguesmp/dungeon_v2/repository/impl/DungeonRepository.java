package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.repository.IDungeonRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DungeonRepository implements IDungeonRepository {

    private final File dungeonTemplateFolder;
    private final Gson gson;

    public DungeonRepository(Plugin plugin, Gson gson) {
        this.gson = gson;
        this.dungeonTemplateFolder = new File(plugin.getDataFolder(), DataFolderConfig.getDungeonTemplateFolder());
        if(!dungeonTemplateFolder.exists()) dungeonTemplateFolder.mkdirs();
    }

    @Override
    public List<Dungeon> loadAll() {
        List<Dungeon> dungeons = new ArrayList<>();
        File[] files = dungeonTemplateFolder.listFiles((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE));
        if(files == null) return dungeons;

        for(File file : files){
            try (Reader reader = Files.newBufferedReader(file.toPath())){
                Dungeon dungeon = gson.fromJson(reader, Dungeon.class);
                if(dungeon != null && dungeon.getId() != null){
                    dungeons.add(dungeon);
                }
            }catch (Exception e){
                Log4Craft.fire("Failed to load dungeon file: " + file.getName(), e);
            }
        }
        return List.of();
    }

    @Override
    public boolean delete(String id) {
        File file = toFile(id);
        if (file.exists() && !file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
        return true;
    }

    @Override
    public Dungeon save(Dungeon dungeon) {
        File file = new File(dungeonTemplateFolder, dungeon.getId() + DataConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(dungeon, writer);
        }catch (Exception e){
            throw new DataSaveException(file.getName(), e);
        }
        return dungeon;
    }

    private File toFile(String id) {
        return new File(dungeonTemplateFolder, id + DataFolderConfig.JSON_TYPE);
    }
}
