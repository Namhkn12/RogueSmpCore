package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldKey;
import com.roguesmp.world.PolarWorldManager;
import com.roguesmp.world.WorldEvacuator;
import live.minehub.polarpaper.core.config.Config;
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

    public static final String NAMESPACE = "island";
    public static final String WORLD_PREFIX = NAMESPACE + "_";
    public static final int BORDER_DIAMETER = 160;
    public static final int CENTER_Y = 65;

    private static final String WORLD_FOLDER_NAME = "island_worlds";
    private static final String TEMPLATE_NAME = "private_island";
    private static final int AUTO_SAVE_TICKS = 6000;

    private final PolarWorldManager backend;

    @Blocking
    public IslandWorldManager(RogueSmpCore plugin) {
        this.backend = new PolarWorldManager(new File(plugin.getDataFolder(), WORLD_FOLDER_NAME).toPath(), NAMESPACE, key -> buildConfig(), (key, world) -> applyBorder(world), WorldEvacuator.none());
        this.backend.sweepOrphans(key -> isIslandId(key.name()));
    }

    private static boolean isIslandId(String name) {
        try {
            UUID.fromString(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private PolarWorldKey key(UUID islandId) {
        return backend.key(islandId.toString());
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

    public String getRegionDataName(UUID islandId) {
        return backend.regionDataName(key(islandId));
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
        return backend.getLoaded(key(islandId));
    }

    public Location getDefaultSpawn(@Nullable World world) {
        return new Location(world, 0.5, CENTER_Y + 2, 0.5);
    }

    public CompletableFuture<Boolean> isWorldAvailable(UUID islandId) {
        return backend.exists(key(islandId));
    }

    public CompletableFuture<World> createIslandWorld(UUID islandId) {
        PolarWorldManager templates = PolarTemplateManager.getInstance().worlds();
        PolarWorldKey template = templates.key(TEMPLATE_NAME);
        return templates.exists(template).thenCompose(hasTemplate -> hasTemplate
                ? backend.copyFrom(templates, template, key(islandId))
                : backend.create(key(islandId), null, world -> generateBaselineIslandStructure(new Location(world, 0, CENTER_Y, 0))));
    }

    public CompletableFuture<World> loadIslandWorld(UUID islandId) {
        return backend.load(key(islandId));
    }

    public void unloadIslandWorld(UUID islandId) {
        backend.unload(key(islandId), true);
    }

    @Blocking
    public void saveAllOnStop() {
        backend.saveAllOnStop();
    }

    public CompletableFuture<Void> discardWorld(World world) {
        return backend.discard(world);
    }

    public CompletableFuture<Void> deleteWorld(UUID islandId) {
        return backend.delete(key(islandId));
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
