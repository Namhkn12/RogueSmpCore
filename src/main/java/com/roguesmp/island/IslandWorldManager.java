package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.bukkit.*;
import org.jetbrains.annotations.Blocking;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.AllowedPortalType;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.LoadWorldOptions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class IslandWorldManager {

    public static final String WORLD_NAME = "sb_island_world";

    private static final int ISLAND_GAP = 400; // Distance between islands
    private static final int GRID_WIDTH = 50; // How many islands per row before dropping down
    private static final String INDEX_FILE_NAME = "island_grid_index.json";

    private int nextAvailableIndex = 0;

    private final RogueSmpCore plugin;
    private final MultiverseCoreApi multiverseApi;

    public IslandWorldManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.multiverseApi = MultiverseCoreApi.get();
        loadGlobalIndexCounter();
    }

    public void loadSkyblockWorld() {
        if (multiverseApi.getWorldManager().isLoadedWorld(WORLD_NAME)) return;
        if (!multiverseApi.getWorldManager().isWorld(WORLD_NAME)) {
            multiverseApi.getWorldManager().createWorld(CreateWorldOptions.worldName(WORLD_NAME)
                            .generatorSettings("{\"layers\":[{\"block\":\"air\",\"height\":1}],\"biome\":\"the_void\"}")
                            .environment(World.Environment.NORMAL)
                            .worldType(WorldType.FLAT)
                            .generateStructures(false))
                    .peek(loadedMultiverseWorld -> {
                        loadedMultiverseWorld.setAllowAdvancementGrant(false);
                        loadedMultiverseWorld.setBedRespawn(false);
                        loadedMultiverseWorld.setAdjustSpawn(false);
                        loadedMultiverseWorld.setAnchorSpawn(false);
                        loadedMultiverseWorld.setDifficulty(Difficulty.NORMAL);
                        loadedMultiverseWorld.setGameMode(GameMode.SURVIVAL);
                        loadedMultiverseWorld.setKeepSpawnInMemory(false);
                        loadedMultiverseWorld.setPortalForm(AllowedPortalType.NONE);
                        loadedMultiverseWorld.setAutoLoad(true);
                    });
        } else {
            multiverseApi.getWorldManager().loadWorld(LoadWorldOptions.world(multiverseApi.getWorldManager().getWorld(WORLD_NAME).get()));
        }

    }

    public int requestNextFreeIndex() {
        int index = this.nextAvailableIndex;
        this.nextAvailableIndex++;
        Utils.runAsync(this::saveGlobalIndexCounter); // Persist the updated counter value immediately
        return index;
    }

    public Location getIslandCenter(int islandGridIndex) {
        World world = getIslandWorld(islandGridIndex);

        int row = islandGridIndex / GRID_WIDTH;
        int col = islandGridIndex % GRID_WIDTH;

        double x = col * ISLAND_GAP;
        double y = 64;
        double z = row * ISLAND_GAP;

        return new Location(world, x, y, z);
    }

    public Location getIslandCenter(IslandData islandData) {
        return getIslandCenter(islandData.getGridIndex());
    }

    /**
     * Method to get island world, we have single world so it's just a world name look up for now.
     */
    public World getIslandWorld(int gridIndex) {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            loadSkyblockWorld();
            throw new IllegalStateException(WORLD_NAME + " does not exist! Will attempt to create a new one...");
        }
        return world;
    }

    /**
     * Method to get island world, we have single world so it's just a world name look up for now.
     */
    public World getIslandWorld(IslandData islandData) {
        return getIslandWorld(islandData.getGridIndex());
    }

    private @Blocking void loadGlobalIndexCounter() {
        File file = new File(plugin.getDataFolder(), INDEX_FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            IndexTracker tracker = Utils.GSON.fromJson(reader, IndexTracker.class);
            if (tracker != null) {
                this.nextAvailableIndex = tracker.next_available_index;
            }
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load global skyblock grid index counter", e);
        }
    }

    private @Blocking void saveGlobalIndexCounter() {
        File file = new File(plugin.getDataFolder(), INDEX_FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            Utils.GSON.toJson(new IndexTracker(this.nextAvailableIndex), writer);
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to save global skyblock grid index counter", e);
        }
    }

    public MultiverseCoreApi getMultiverseApi() {
        return multiverseApi;
    }

    public void onDisable() {
        saveGlobalIndexCounter();
    }

    // Small DTO helper class matching GSON formatting requirements cleanly
    private static class IndexTracker {
        int next_available_index;
        public IndexTracker(int value) { this.next_available_index = value; }
    }
}
