package com.roguesmp.world;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.AsyncUtils;
import live.minehub.polarpaper.Polar;
import live.minehub.polarpaper.core.config.Config;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Every method is non-blocking and must be called from the main thread, except the constructor path in {@link #init}.
 */
public final class PolarTemplateManager {

    private static final String FOLDER_NAME = "polar_template_worlds";
    public static final String NAMESPACE = "template";

    private static PolarTemplateManager INSTANCE;

    private final RogueSmpCore plugin;
    private final PolarWorldManager worldManager;

    @Blocking
    private PolarTemplateManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.worldManager = new PolarWorldManager(plugin.getDataFolder().toPath().resolve(FOLDER_NAME), NAMESPACE, this::configFor, this::applyBorder, WorldEvacuator.toDefaultWorld());
        this.worldManager.sweepOrphans(key -> true);
    }

    @Blocking
    public static void init(RogueSmpCore plugin) {
        INSTANCE = new PolarTemplateManager(plugin);
    }

    public static PolarTemplateManager getInstance() {
        return INSTANCE;
    }

    public PolarWorldManager worlds() {
        return worldManager;
    }

    public PolarTemplateData data(PolarWorldKey key) {
        PolarTemplateData data = Registries.POLAR_TEMPLATE_DATA.get(key.name());
        return data != null ? data : PolarTemplateData.DEFAULT;
    }

    public @Nullable PolarWorldKey keyOf(World world) {
        return worldManager.getManagedKey(world);
    }

    public CompletableFuture<World> create(PolarWorldKey key, @Nullable PolarWorldKey template) {
        if (template == null) return worldManager.create(key, null, world -> { });

        return worldManager.readStored(template).thenCompose(base -> {
            if (base == null) {
                return CompletableFuture.<World>failedFuture(new IllegalArgumentException("Template world not found: " + template));
            }
            return cloneOf(template, key, () -> worldManager.create(key, base, world -> { }));
        });
    }

    public CompletableFuture<World> load(PolarWorldKey key) {
        return worldManager.load(key);
    }

    public CompletableFuture<Void> save(PolarWorldKey key) {
        World world = worldManager.getLoaded(key);
        if (world != null) ServerRegionManager.getInstance().saveChanges(world);
        return worldManager.save(key);
    }

    /**
     * Saves a world as a template. A loaded template world is saved in place; any other Polar world, such as one not
     * created by this manager, is exported as a new template, with its WorldGuard regions and spawn, under {@code target}.
     *
     * @return the key of the template that was saved
     */
    public CompletableFuture<PolarWorldKey> saveWorld(World world, PolarWorldKey target) {
        PolarWorldKey managed = worldManager.getManagedKey(world);

        if (worldManager.getLoaded(target) != null) return save(target).thenApply(v -> target);
        if (managed != null) return CompletableFuture.failedFuture(new IllegalStateException("Template world is not loaded: " + target));
        if (!PolarWorldManager.isPolar(world)) {
            return CompletableFuture.failedFuture(new IllegalStateException("This is not a Polar world, so it cannot be saved as a template."));
        }

        return worldManager.serialized(target, () -> worldManager.exists(target).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.<PolarWorldKey>failedFuture(new IllegalStateException("Template " + target + " already exists, delete it first or pick another name."));
            }
            return exportAsTemplate(world, target);
        }));
    }

    public CompletableFuture<Void> unload(PolarWorldKey key, boolean save) {
        return worldManager.unload(key, save);
    }

    public CompletableFuture<Void> delete(PolarWorldKey key) {
        return worldManager.delete(key)
                .thenCompose(v -> deleteRegionData(key))
                .thenCompose(v -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA.removeAndDeleteFilesAsync(plugin, key.name())))
                .thenAccept(removed -> { });
    }

    public CompletableFuture<World> copy(PolarWorldKey source, PolarWorldKey target) {
        return cloneOf(source, target, () -> worldManager.copyFrom(worldManager, source, target));
    }

    public CompletableFuture<Void> setSpawn(PolarWorldKey key, Location spawn) {
        World world = worldManager.getLoaded(key);
        if (world != null) {
            Location located = spawn.clone();
            located.setWorld(world);
            world.setSpawnLocation(located);
        }
        return Registries.POLAR_TEMPLATE_DATA.registerAndSaveAsync(plugin, key.name(), data(key).withSpawn(spawn)).thenAccept(saved -> { });
    }

    public CompletableFuture<Void> setBorder(PolarWorldKey key, double diameter) {
        return worldManager.exists(key).thenCompose(exists -> {
            if (!exists) return CompletableFuture.<Void>failedFuture(new IllegalArgumentException("Unknown template world: " + key));

            return AsyncUtils.onMainThread(() -> {
                CompletableFuture<Void> saved = Registries.POLAR_TEMPLATE_DATA
                        .registerAndSaveAsync(plugin, key.name(), data(key).withBorderDiameter(diameter))
                        .thenAccept(data -> { });

                World world = worldManager.getLoaded(key);
                if (world == null) return saved;
                if (diameter <= 0) world.getWorldBorder().reset();
                else applyBorder(key, world);
                return saved;
            });
        });
    }

    private CompletableFuture<PolarWorldKey> exportAsTemplate(World world, PolarWorldKey target) {
        String sourceDataName = world.getKey().getNamespace() + "_" + world.getKey().getKey();
        Location spawn = world.getSpawnLocation();

        return AsyncUtils.onMainThread(() -> {
            ServerRegionManager.getInstance().saveChanges(world);
            return Polar.saveWorld(world, worldManager.source(target));
        })
                .thenCompose(v -> AsyncUtils.run(() -> ServerRegionManager.getInstance().copyStoredData(sourceDataName, worldManager.regionDataName(target))))
                .thenCompose(v -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA
                        .registerAndSaveAsync(plugin, target.name(), PolarTemplateData.DEFAULT.withSpawn(spawn))))
                .thenApply(saved -> target);
    }

    public CompletableFuture<Void> saveRegions(PolarWorldKey key) {
        return AsyncUtils.onMainThread(() -> {
            World world = worldManager.getLoaded(key);
            if (world != null) ServerRegionManager.getInstance().saveChanges(world);
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Copies the saved regions data and things like border, spawn of {@code source} to {@code target} before the world is
     * created. The target must not exist yet, or its own regions would be overwritten.
     * A failed creation removes what was copied.
     */
    private CompletableFuture<World> cloneOf(PolarWorldKey source, PolarWorldKey target, Supplier<CompletableFuture<World>> creation) {
        return worldManager.exists(target).thenCompose(exists -> {
            if (exists) return CompletableFuture.<World>failedFuture(new IllegalStateException("World already exists: " + target));

            return saveRegions(source)
                    .thenCompose(v -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA.registerAndSaveAsync(plugin, target.name(), data(source))))
                    .thenCompose(data -> AsyncUtils.run(() -> ServerRegionManager.getInstance().copyStoredData(worldManager.regionDataName(source), worldManager.regionDataName(target))))
                    .thenCompose(v -> creation.get())
                    .exceptionallyCompose(error -> discardClone(target).handle((v, cleanupError) -> null)
                            .thenCompose(v -> CompletableFuture.<World>failedFuture(error)));
        });
    }

    private CompletableFuture<Void> discardClone(PolarWorldKey target) {
        return worldManager.exists(target).thenCompose(exists -> {
            if (exists) return CompletableFuture.<Void>completedFuture(null);
            return deleteRegionData(target)
                    .thenCompose(v -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA.removeAndDeleteFilesAsync(plugin, target.name())))
                    .thenAccept(removed -> { });
        });
    }

    private CompletableFuture<Void> deleteRegionData(PolarWorldKey key) {
        return AsyncUtils.run(() -> ServerRegionManager.getInstance().deleteStoredData(worldManager.regionDataName(key)));
    }

    private Config configFor(PolarWorldKey key) {
        return Config.Builder.defaults()
                .spawn(data(key).spawn())
                .loadOnStartup(false)
                .saveOnStop(false)
                .autoSaveIntervalTicks(-1)
                .announceAutosave(false)
                .build();
    }

    private void applyBorder(PolarWorldKey key, World world) {
        double diameter = data(key).borderDiameter();
        if (diameter <= 0) return;
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(diameter);
    }

}
