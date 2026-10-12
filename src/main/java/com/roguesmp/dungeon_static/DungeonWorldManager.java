package com.roguesmp.dungeon_static;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.world.PolarTemplateData;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldKey;
import com.roguesmp.world.PolarWorldManager;
import com.roguesmp.world.WorldEvacuator;
import live.minehub.polarpaper.core.config.Config;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent dungeon worlds cloned from polar templates. An instance world is named {@code <template name>.<instanceId>},
 * so its template (spawn, border) can always be recovered from the key.
 */
public class DungeonWorldManager {

    private static final String WORLD_FOLDER = "dungeon_worlds";
    private static final String NAMESPACE = "dungeon";
    private static final char INSTANCE_SEPARATOR = '.';
    private static final int AUTO_SAVE_TICKS = 6000;

    private final PolarTemplateManager templates;
    private final PolarWorldManager worlds;
    private final Map<UUID, PolarTemplateData> worldData = new ConcurrentHashMap<>();

    @Blocking
    public DungeonWorldManager(RogueSmpCore plugin, PolarTemplateManager templates) {
        this.templates = templates;
        this.worlds = new PolarWorldManager(new File(plugin.getDataFolder(), WORLD_FOLDER).toPath(), NAMESPACE, this::buildConfig, this::applyBorder, WorldEvacuator.toDefaultWorld());
    }

    @Blocking
    public void saveAllOnStop() {
        worlds.saveAllOnStop();
    }

    public PolarWorldKey worldKey(PolarWorldKey template, UUID instanceId) {
        return worlds.key(template.name() + INSTANCE_SEPARATOR + instanceId);
    }

    public PolarTemplateData snapshotOf(PolarWorldKey template) {
        return templates.data(template);
    }

    public void remember(UUID instanceId, PolarTemplateData data) {
        worldData.put(instanceId, data);
    }

    /**
     * Copies the template's saved WorldGuard region data, then the world itself, which loads with {@code data} (spawn,
     * border). A failed creation removes what was copied.
     */
    public CompletableFuture<World> create(PolarWorldKey template, UUID instanceId, PolarTemplateData data) {
        PolarWorldKey instance = worldKey(template, instanceId);
        remember(instanceId, data);

        return templates.saveRegions(template)
                .thenCompose(v -> AsyncUtils.run(() -> ServerRegionManager.getInstance().copyStoredData(templates.worlds().regionDataName(template), worlds.regionDataName(instance))))
                .thenCompose(v -> worlds.copyFrom(templates.worlds(), template, instance))
                .exceptionallyCompose(error -> discardPartial(instance, instanceId).handle((v, cleanupError) -> null)
                        .thenCompose(v -> CompletableFuture.<World>failedFuture(error)));
    }

    private CompletableFuture<Void> discardPartial(PolarWorldKey instance, UUID instanceId) {
        return worlds.exists(instance).thenCompose(exists -> {
            if (exists) return CompletableFuture.<Void>completedFuture(null);
            worldData.remove(instanceId);
            return deleteRegionData(instance);
        });
    }

    private PolarTemplateData dataOf(PolarWorldKey instance) {
        UUID instanceId = parseInstanceId(instance);
        PolarTemplateData data = instanceId == null ? null : worldData.get(instanceId);
        return data != null ? data : templates.data(templateOf(instance));
    }

    public CompletableFuture<World> load(PolarWorldKey template, UUID instanceId) {
        return worlds.load(worldKey(template, instanceId));
    }

    public CompletableFuture<Void> unload(PolarWorldKey template, UUID instanceId) {
        return worlds.unload(worldKey(template, instanceId), true);
    }

    public CompletableFuture<Void> delete(PolarWorldKey template, UUID instanceId) {
        PolarWorldKey instance = worldKey(template, instanceId);
        return worlds.delete(instance).thenCompose(v -> deleteRegionData(instance)).thenAccept(v -> worldData.remove(instanceId));
    }

    public CompletableFuture<Boolean> exists(PolarWorldKey template, UUID instanceId) {
        return worlds.exists(worldKey(template, instanceId));
    }

    public @Nullable World getLoaded(PolarWorldKey template, UUID instanceId) {
        return worlds.getLoaded(worldKey(template, instanceId));
    }

    /**
     * Deletes every dungeon world whose instance id is not in {@code knownInstanceIds}, which are leftovers of a crash
     * between creating a world and recording its instance, then sweeps the region data they left. Worlds whose name does
     * not parse as an instance are kept.
     *
     * @return how many worlds were deleted
     */
    @Blocking
    public int deleteOrphans(Set<UUID> knownInstanceIds) {
        int deleted = worlds.deleteStoredWhere(key -> {
            UUID instanceId = parseInstanceId(key);
            return instanceId != null && !knownInstanceIds.contains(instanceId);
        });
        worlds.sweepOrphans(key -> parseInstanceId(key) != null);
        return deleted;
    }

    public boolean isDungeonWorld(World world) {
        return worlds.getManagedKey(world) != null;
    }

    /**
     * @param worldKeyValue a PolarPaper world key such as {@code dungeon_<template>.<instanceId>}, with or without the namespace in front, which may belong to a world that is not loaded
     */
    public @Nullable UUID getInstanceId(String worldKeyValue) {
        PolarWorldKey key = worlds.keyOf(Key.key(worldKeyValue).value());
        return key == null ? null : parseInstanceId(key);
    }

    public @Nullable UUID getInstanceId(World world) {
        PolarWorldKey key = worlds.getManagedKey(world);
        return key == null ? null : parseInstanceId(key);
    }

    private @Nullable UUID parseInstanceId(PolarWorldKey instance) {
        return parseInstanceId(instance.name());
    }

    private @Nullable UUID parseInstanceId(String name) {
        try {
            return UUID.fromString(name.substring(name.lastIndexOf(INSTANCE_SEPARATOR) + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private CompletableFuture<Void> deleteRegionData(PolarWorldKey instance) {
        return AsyncUtils.run(() -> ServerRegionManager.getInstance().deleteStoredData(worlds.regionDataName(instance)));
    }

    private PolarWorldKey templateOf(PolarWorldKey instance) {
        String name = instance.name();
        int separator = name.lastIndexOf(INSTANCE_SEPARATOR);
        return templates.worlds().key(separator < 0 ? name : name.substring(0, separator));
    }

    private Config buildConfig(PolarWorldKey instance) {
        PolarTemplateData data = dataOf(instance);
        return Config.Builder.defaults()
                .spawn(data.spawn())
                .loadOnStartup(false)
                .saveOnStop(true)
                .autoSaveIntervalTicks(AUTO_SAVE_TICKS)
                .announceAutosave(false)
                .build();
    }

    private void applyBorder(PolarWorldKey instance, World world) {
        double diameter = dataOf(instance).borderDiameter();
        if (diameter <= 0) return;
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(diameter);
    }
}
