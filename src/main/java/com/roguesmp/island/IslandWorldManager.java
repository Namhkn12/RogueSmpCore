package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.world.PolarWorldManager;
import live.minehub.polarpaper.core.config.Config;
import live.minehub.polarpaper.core.source.FilePolarSource;
import live.minehub.polarpaper.core.world.PolarReader;
import live.minehub.polarpaper.core.world.PolarWorld;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IslandWorldManager {

    public static final String WORLD_PREFIX = "island_";
    public static final int BORDER_DIAMETER = 160;
    public static final int CENTER_Y = 65;

    private static final String WORLD_FOLDER_NAME = "island_worlds";
    private static final String TEMPLATE_FILE_NAME = "island_template.polar";
    private static final int AUTO_SAVE_TICKS = 6000;

    private final RogueSmpCore plugin;
    private final PolarWorldManager backend;

    @Blocking
    public IslandWorldManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.backend = new PolarWorldManager(new File(plugin.getDataFolder(), WORLD_FOLDER_NAME).toPath(), name -> buildConfig(), this::applyBorder);
    }

    @Blocking
    private @Nullable PolarWorld readTemplate() {
        File file = new File(plugin.getDataFolder(), TEMPLATE_FILE_NAME);
        if (!file.exists()) return null;

        try {
            return new PolarReader().read(new FilePolarSource(file.toPath()));
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to read island template {}", file.getPath(), e);
            return null;
        }
    }

    private Config buildConfig() {
        return Config.Builder.defaults()
                .spawn(getDefaultSpawn(null))
                .loadOnStartup(false)
                .saveOnStop(true)
                .autoSaveIntervalTicks(AUTO_SAVE_TICKS)
                .announceAutosave(false)
                .build();
    }

    private void applyBorder(World world) {
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(BORDER_DIAMETER);
    }

    public String getWorldName(UUID islandId) {
        return WORLD_PREFIX + islandId;
    }

    public boolean isIslandWorld(World world) {
        return world.getKey().getKey().startsWith(WORLD_PREFIX);
    }

    public @Nullable UUID getIslandId(World world) {
        return getIslandId(world.getKey().asString());
    }

    public @Nullable UUID getIslandId(String worldKey) {
        String worldId = Key.key(worldKey).value();
        if (!worldId.startsWith(WORLD_PREFIX)) return null;
        try {
            return UUID.fromString(worldId.substring(WORLD_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @Nullable World getLoadedWorld(UUID islandId) {
        return backend.getLoaded(getWorldName(islandId));
    }

    public Location getDefaultSpawn(@Nullable World world) {
        return new Location(world, 0.5, CENTER_Y + 2, 0.5);
    }

    public CompletableFuture<Boolean> isWorldAvailable(UUID islandId) {
        return backend.exists(getWorldName(islandId));
    }

    public CompletableFuture<World> createIslandWorld(UUID islandId) {
        return AsyncUtils.supply(this::readTemplate).thenCompose(template -> backend.create(getWorldName(islandId), template, world -> {
            if (template == null) generateBaselineIslandStructure(new Location(world, 0, CENTER_Y, 0));
        }));
    }

    public CompletableFuture<World> loadIslandWorld(UUID islandId) {
        return backend.load(getWorldName(islandId));
    }

    public void unloadIslandWorld(UUID islandId) {
        backend.unload(getWorldName(islandId), true);
    }

    public CompletableFuture<Void> discardWorld(World world) {
        return backend.discard(world);
    }

    @Blocking
    public void deleteWorldFile(UUID islandId) {
        try {
            backend.deleteStored(getWorldName(islandId));
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to delete island world file for {}", islandId, e);
        }
    }

    private void generateBaselineIslandStructure(Location center) {
        center.getChunk().load(true);
        center.getBlock().setType(Material.BEDROCK);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                center.clone().add(x, 0, z).getBlock().setType(Material.DIRT);
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                center.clone().add(x, 1, z).getBlock().setType(Material.GRASS_BLOCK);
            }
        }
    }
}
