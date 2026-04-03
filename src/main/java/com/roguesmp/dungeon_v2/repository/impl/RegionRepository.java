package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.DungeonWorld;
import com.roguesmp.dungeon_v2.repository.IRegionRepository;
import com.roguesmp.dungeon_v2.utils_.Log4Craft_;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegionRepository implements IRegionRepository {

    private final Gson gson;
    private final File regionFolder;
    private final Log4Craft_ logger;

    public RegionRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.gson = gson;

        // dungeon/region/ relative to plugin data folder
        this.regionFolder = new File(
                plugin.getDataFolder(),
                DataFolderConfig.getRegionFolder()
        );
        this.logger = logger;

        if (!regionFolder.exists()) {
            regionFolder.mkdirs();
        }
    }

    @Override
    public List<DungeonWorld> loadAll() {
        List<DungeonWorld> result = new ArrayList<>();
        File[] files = regionFolder.listFiles(
                (dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)
        );

        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                DungeonWorld world = gson.fromJson(reader, DungeonWorld.class);
                if (world != null) {
                    result.add(world);
                }
            } catch (IOException e) {
                logger.fire(this.getClass(), "Failed to load region file: " + file.getName(), e);
            }
        }

        return result;
    }

    @Override
    public void save(DungeonWorld dungeonWorld) {
        File file = getFile(dungeonWorld.getWorldName());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(dungeonWorld, writer);
        } catch (IOException e) {
            logger.fire(this.getClass(), "Failed to save region file for world: " + dungeonWorld.getWorldName(), e);
        }
    }

    @Override
    public Optional<DungeonWorld> loadByWorldName(String worldName) {
        File file = getFile(worldName);
        if (!file.exists()) return Optional.empty();

        try (Reader reader = new FileReader(file)) {
            return Optional.ofNullable(gson.fromJson(reader, DungeonWorld.class));
        } catch (IOException e) {
            logger.fire(this.getClass(), "Failed to load region file for world: " + worldName, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String worldName) {
        File file = getFile(worldName);
        if (file.exists() && !file.delete()) {
            logger.warn(this.getClass(), "Failed to delete region file for world: " + worldName);

        }
    }

    // tên file = worldName.json
    private File getFile(String worldName) {
        return new File(regionFolder, DataFolderConfig.REGION_INSTANCE_FILE + worldName + DataFolderConfig.JSON_TYPE);
    }
}
