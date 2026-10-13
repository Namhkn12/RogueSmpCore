package com.roguesmp.dungeon.repository;

import com.roguesmp.RogueSmpCore;
import com.google.gson.Gson;
import com.roguesmp.dungeon.config.DataFolderConfig;
import com.roguesmp.dungeon.data.definition.Dungeon;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DungeonRepository {

    private final File dungeonTemplateFolder;
    private final Gson gson;

    public DungeonRepository(Plugin plugin, Gson gson) {
        this.gson = gson;
        this.dungeonTemplateFolder = new File(plugin.getDataFolder(), DataFolderConfig.getDungeonTemplateFolder());
        if(!dungeonTemplateFolder.exists()) dungeonTemplateFolder.mkdirs();
    }

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
                RogueSmpCore.LOGGER.error("Failed to load dungeon file: {}", file.getName(), e);
            }
        }
        return dungeons;
    }

    public boolean delete(String id) {
        File file = toFile(id);
        if (file.exists() && !file.delete()) {
            RogueSmpCore.LOGGER.error("Failed to delete file: {}", file.getName());
            return false;
        }
        return true;
    }

    public Dungeon save(Dungeon dungeon) {
        File file = new File(dungeonTemplateFolder, dungeon.getId() + DataFolderConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(dungeon, writer);
        }catch (Exception e){
            RogueSmpCore.LOGGER.error("Failed to save file: {}", file.getName(), e);
            return null;
        }
        return dungeon;
    }

    private File toFile(String id) {
        return new File(dungeonTemplateFolder, id + DataFolderConfig.JSON_TYPE);
    }
}
