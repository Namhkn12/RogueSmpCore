package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.DungeonWorld;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.repository.IRegionRepository;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class RegionRepository implements IRegionRepository {

    private final Gson gson;
    private final File regionFolder;

    public RegionRepository() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // dungeon/region/ relative to plugin data folder
        this.regionFolder = new File(
                RogueSmpCore.getInstance().getDataFolder(),
                DataConfig.getRegionFolder()
        );

        if (!regionFolder.exists()) {
            regionFolder.mkdirs();
        }
    }

    @Override
    public List<DungeonWorld> loadAll() {
        List<DungeonWorld> result = new ArrayList<>();
        File[] files = regionFolder.listFiles(
                (dir, name) -> name.endsWith(DataConfig.JSON_TYPE)
        );

        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                DungeonWorld world = gson.fromJson(reader, DungeonWorld.class);
                if (world != null) {
                    result.add(world);
                }
            } catch (IOException e) {
                RogueSmpCore.getInstance().getLogger().log(
                        Level.SEVERE,
                        "Failed to load region file: " + file.getName(), e
                );
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
            Log4Craft.fire("Failed to save region file for world: " + dungeonWorld.getWorldName(), e);
        }
    }

    @Override
    public Optional<DungeonWorld> loadByWorldName(String worldName) {
        File file = getFile(worldName);
        if (!file.exists()) return Optional.empty();

        try (Reader reader = new FileReader(file)) {
            return Optional.ofNullable(gson.fromJson(reader, DungeonWorld.class));
        } catch (IOException e) {
            RogueSmpCore.getInstance().getLogger().log(
                    Level.SEVERE,
                    "Failed to load region file for world: " + worldName, e
            );
            return Optional.empty();
        }
    }

    @Override
    public void delete(String worldName) {
        File file = getFile(worldName);
        if (file.exists() && !file.delete()) {
            RogueSmpCore.getInstance().getLogger().warning(
                    "Failed to delete region file for world: " + worldName
            );
        }
    }

    // tên file = worldName.json
    private File getFile(String worldName) {
        return new File(regionFolder, DataConfig.REGION_INSTANCE_FILE + worldName + DataConfig.JSON_TYPE);
    }
}
