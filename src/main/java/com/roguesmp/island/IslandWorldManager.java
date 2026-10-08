package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import live.minehub.polarpaper.Polar;
import live.minehub.polarpaper.core.config.Config;
import live.minehub.polarpaper.core.generator.PolarGenerator;
import live.minehub.polarpaper.core.source.FilePolarSource;
import live.minehub.polarpaper.core.source.PolarSource;
import live.minehub.polarpaper.core.world.PolarReader;
import live.minehub.polarpaper.core.world.PolarWorld;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class IslandWorldManager {

    public static final String WORLD_PREFIX = "island_";
    public static final int BORDER_DIAMETER = 160;

    private static final String WORLD_FOLDER_NAME = "island_worlds";
    private static final String TEMPLATE_FILE_NAME = "island_template.polar";
    private static final byte MIN_SECTION = -4;
    private static final byte MAX_SECTION = 19;
    private static final int AUTO_SAVE_TICKS = 6000;
    public static final int CENTER_Y = 65;
    private static final int EVACUATION_DELAY_TICKS = 20;

    private final RogueSmpCore plugin;
    private final File worldFolder;
    private final PolarWorld template;
    private final Map<UUID, CompletableFuture<World>> loading = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> unloading = new ConcurrentHashMap<>();

    public IslandWorldManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.worldFolder = new File(plugin.getDataFolder(), WORLD_FOLDER_NAME);
        this.worldFolder.mkdirs();
        this.template = readTemplate();
    }

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

    public FilePolarSource getWorldSource(String worldName) {
        return new FilePolarSource(worldFolder.toPath().resolve(worldName + ".polar"));
    }

    public String getWorldName(UUID islandId) {
        return WORLD_PREFIX + islandId;
    }

    private String getWorldId(World world) {
        return world.getKey().getKey();
    }

    public boolean isIslandWorld(World world) {
        return getWorldId(world).startsWith(WORLD_PREFIX);
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
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null) return null;
        return Bukkit.getWorld(new NamespacedKey(polarPlugin, getWorldName(islandId)));
    }

    public Location getDefaultSpawn(World world) {
        return new Location(world, 0.5, CENTER_Y + 2, 0.5);
    }

    public boolean isWorldAvailable(UUID islandId) {
        return getLoadedWorld(islandId) != null || Files.exists(getWorldSource(getWorldName(islandId)).path());
    }

    public CompletableFuture<World> createIslandWorld(UUID islandId) {
        String worldName = getWorldName(islandId);
        PolarWorld polarWorld = template != null ? template : new PolarWorld(MIN_SECTION, MAX_SECTION);

        return Polar.createWorld(polarWorld, worldName, buildConfig())
                .thenCompose(world -> {
                    CompletableFuture<Void> prepared = new CompletableFuture<>();
                    Utils.runLater(() -> {
                        PolarGenerator.fromWorld(world).setSource(getWorldSource(worldName));
                        if (template == null) generateBaselineIslandStructure(new Location(world, 0, CENTER_Y, 0));
                        world.getWorldBorder().setCenter(0, 0);
                        world.getWorldBorder().setSize(BORDER_DIAMETER);
                        prepared.complete(null);
                    });
                    return prepared.thenCompose(v -> Polar.saveWorld(world, getWorldSource(worldName))).thenApply(v -> world);
                });
    }

    public CompletableFuture<World> loadIslandWorld(UUID islandId) {
        if (!Bukkit.isPrimaryThread()) {
            CompletableFuture<World> result = new CompletableFuture<>();
            Utils.runLater(() -> loadIslandWorld(islandId).whenComplete((world, error) -> {
                if (error != null) result.completeExceptionally(error);
                else result.complete(world);
            }));
            return result;
        }

        CompletableFuture<Void> pendingUnload = unloading.get(islandId);
        if (pendingUnload != null) return pendingUnload.thenCompose(v -> loadIslandWorld(islandId));

        World loaded = getLoadedWorld(islandId);
        if (loaded != null) return CompletableFuture.completedFuture(loaded);

        if (!isWorldAvailable(islandId)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Island world file is missing for " + islandId));
        }

        CompletableFuture<World> inFlight = loading.get(islandId);
        if (inFlight != null) return inFlight;

        String worldName = getWorldName(islandId);
        PolarSource source = getWorldSource(worldName);
        CompletableFuture<World> future = Polar.createWorld(source, worldName, buildConfig());
        loading.put(islandId, future);
        future.whenComplete((world, error) -> Utils.runLater(() -> {
            loading.remove(islandId);
            if (error != null) RogueSmpCore.LOGGER.error("Failed to load island world {}", worldName, error);
            else if (world == null) RogueSmpCore.LOGGER.error("Island world {} was not created by Polar", worldName);
            else {
                world.getWorldBorder().setCenter(0, 0);
                world.getWorldBorder().setSize(BORDER_DIAMETER);
            }
        }));
        return future;
    }

    public void unloadIslandWorld(UUID islandId) {
        World world = getLoadedWorld(islandId);
        if (world == null || unloading.containsKey(islandId)) return;

        CompletableFuture<Void> pendingUnload = new CompletableFuture<>();
        unloading.put(islandId, pendingUnload);
        Polar.saveWorld(world, getWorldSource(getWorldId(world)))
                .thenRun(() -> Utils.runLater(() -> {
                    Bukkit.unloadWorld(world, false);
                    unloading.remove(islandId);
                    pendingUnload.complete(null);
                }))
                .exceptionally(error -> {
                    RogueSmpCore.LOGGER.error("Failed to save island world {}", getWorldId(world), error);
                    Utils.runLater(() -> {
                        unloading.remove(islandId);
                        pendingUnload.complete(null);
                    });
                    return null;
                });
    }

    public CompletableFuture<Void> discardWorld(World world) {
        UUID islandId = getIslandId(world);
        CompletableFuture<Void> pendingUnload = islandId == null ? null : unloading.get(islandId);
        if (pendingUnload != null) return pendingUnload;

        Polar.stopAutoSaveTask(world.getKey());

        CompletableFuture<Void> discarded = new CompletableFuture<>();
        Utils.runLater(() -> {
            Bukkit.unloadWorld(world, false);
            discarded.complete(null);
        }, EVACUATION_DELAY_TICKS);
        return discarded;
    }

    public void deleteWorldFile(UUID islandId) {
        try {
            getWorldSource(getWorldName(islandId)).delete();
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to delete island world file for {}", islandId, e);
        }
    }

    private Config buildConfig() {
        return Config.Builder.defaults()
                .spawn(new Location(null, 0.5, CENTER_Y + 2, 0.5))
                .loadOnStartup(false)
                .saveOnStop(true)
                .autoSaveIntervalTicks(AUTO_SAVE_TICKS)
                .announceAutosave(false)
                .build();
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
