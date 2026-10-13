package com.roguesmp.world;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.utils.Utils;
import live.minehub.polarpaper.Polar;
import live.minehub.polarpaper.core.config.Config;
import live.minehub.polarpaper.core.generator.PolarGenerator;
import live.minehub.polarpaper.core.source.FilePolarSource;
import live.minehub.polarpaper.core.world.PolarReader;
import live.minehub.polarpaper.core.world.PolarWorld;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Owns every {@code .polar} world file in one folder. Worlds are addressed by {@link PolarWorldKey}, whose namespace is
 * this manager's, so a key from another manager is rejected instead of silently pointing at a different file. Namespaces
 * are claimed at construction and may not overlap, so worlds of different managers can never collide in PolarPaper's
 * global world namespace.
 * <p>
 * Every method that returns a future is non-blocking and safe to call from the main thread. Operations on the same
 * world run one at a time, in call order, so e.g. a delete followed by a create of that world is safe. Methods annotated
 * {@link Blocking} do file IO on the calling thread and must only be called off the main thread.
 */
public final class PolarWorldManager {

    private record Persistence(int autoSaveTicks, boolean saveOnStop) {
    }

    private static final String EXTENSION = ".polar";
    private static final byte MIN_SECTION = -4;
    private static final byte MAX_SECTION = 19;
    private static final int EVACUATION_DELAY_TICKS = 20;
    private static final long SHUTDOWN_SAVE_TIMEOUT_SECONDS = 30;
    private static final Set<String> CLAIMED_PREFIXES = ConcurrentHashMap.newKeySet();

    private final Path folder;
    private final String namespace;
    private final String prefix;
    private final Function<PolarWorldKey, Config> configProvider;
    private final BiConsumer<PolarWorldKey, World> worldSetup;
    private final WorldEvacuator evacuator;
    private final Set<PolarWorldKey> managed = ConcurrentHashMap.newKeySet();
    private final Map<PolarWorldKey, CompletableFuture<?>> tails = new ConcurrentHashMap<>();
    private final Map<PolarWorldKey, Persistence> persistence = new ConcurrentHashMap<>();
    private final Map<PolarWorldKey, BukkitTask> autosaveTasks = new ConcurrentHashMap<>();
    private final Map<PolarWorldKey, CompletableFuture<Void>> savesInFlight = new ConcurrentHashMap<>();

    /**
     * @param namespace      the namespace of every key this manager owns; must not overlap another manager's namespace
     * @param configProvider builds the Polar config (spawn, autosave, ...) for a world, each time it is created or loaded
     * @param worldSetup     applied on the main thread to every world right after it is created or loaded (e.g. world border)
     * @param evacuator      runs inside the operation queue before a loaded world is unloaded, discarded or deleted
     * @throws IllegalStateException if the namespace overlaps another manager's namespace
     */
    @Blocking
    public PolarWorldManager(Path folder, String namespace, Function<PolarWorldKey, Config> configProvider, BiConsumer<PolarWorldKey, World> worldSetup, WorldEvacuator evacuator) {
        new PolarWorldKey(namespace, "validated");
        this.namespace = namespace;
        this.prefix = namespace + PolarWorldKey.VALUE_SEPARATOR;
        claimPrefix(prefix);
        this.folder = folder;
        this.configProvider = configProvider;
        this.worldSetup = worldSetup;
        this.evacuator = evacuator;
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static synchronized void claimPrefix(String prefix) {
        for (String claimed : CLAIMED_PREFIXES) {
            if (claimed.startsWith(prefix) || prefix.startsWith(claimed)) {
                throw new IllegalStateException("Polar world namespace '" + prefix + "' overlaps '" + claimed + "'");
            }
        }
        CLAIMED_PREFIXES.add(prefix);
    }

    public PolarWorldKey key(String name) {
        return new PolarWorldKey(namespace, name);
    }

    /**
     * @return the key a PolarPaper world key (as in {@link World#getKey()}) stands for in this manager, or null if it
     * is not one of this manager's worlds
     */
    public @Nullable PolarWorldKey keyOf(String worldKeyValue) {
        if (!worldKeyValue.startsWith(prefix)) return null;
        try {
            return key(worldKeyValue.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean owns(PolarWorldKey key) {
        return key.namespace().equals(namespace);
    }

    public static boolean isPolar(World world) {
        return PolarGenerator.fromWorld(world) != null;
    }

    public FilePolarSource source(PolarWorldKey key) {
        return new FilePolarSource(folder.resolve(key.value() + EXTENSION));
    }

    public NamespacedKey namespacedKey(PolarWorldKey key) {
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null) throw new IllegalStateException("PolarPaper is not loaded");
        return new NamespacedKey(polarPlugin, key.value());
    }

    public @Nullable World getLoaded(PolarWorldKey key) {
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null || !owns(key)) return null;
        return Bukkit.getWorld(new NamespacedKey(polarPlugin, key.value()));
    }

    /**
     * @return the key of the loaded world if this manager created or loaded it, without touching the disk
     */
    public @Nullable PolarWorldKey getManagedKey(World world) {
        PolarWorldKey key = keyOf(world.getKey().getKey());
        return key != null && managed.contains(key) && world.equals(getLoaded(key)) ? key : null;
    }

    public CompletableFuture<Boolean> exists(PolarWorldKey key) {
        if (!owns(key)) return foreign(key);
        if (getLoaded(key) != null) return CompletableFuture.completedFuture(true);
        return AsyncUtils.supply(() -> isStored(key));
    }

    public CompletableFuture<List<PolarWorldKey>> storedKeys() {
        return AsyncUtils.supply(this::listStoredKeys);
    }

    public CompletableFuture<@Nullable PolarWorld> readStored(PolarWorldKey key) {
        if (!owns(key)) return foreign(key);
        return AsyncUtils.supply(() -> readStoredFile(key));
    }

    @Blocking
    private boolean isStored(PolarWorldKey key) {
        return Files.exists(source(key).path());
    }

    @Blocking
    private List<PolarWorldKey> listStoredKeys() {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(EXTENSION))
                    .map(fileName -> keyOf(fileName.substring(0, fileName.length() - EXTENSION.length())))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(PolarWorldKey::name))
                    .toList();
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to list polar worlds in {}", folder, e);
            return List.of();
        }
    }

    @Blocking
    private @Nullable PolarWorld readStoredFile(PolarWorldKey key) {
        if (!isStored(key)) return null;
        try {
            return new PolarReader().read(source(key));
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to read polar world {}", key, e);
            return null;
        }
    }

    /**
     * Saves every loaded world configured to save on stop and waits for it. Call from the plugin's onDisable.
     */
    @Blocking
    public void saveAllOnStop() {
        List<CompletableFuture<Void>> saves = new ArrayList<>();
        for (PolarWorldKey key : managed) {
            Persistence settings = persistence.get(key);
            World world = getLoaded(key);
            if (settings != null && settings.saveOnStop() && world != null) saves.add(Polar.saveWorld(world, source(key)));
        }

        try {
            CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new)).get(SHUTDOWN_SAVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to save polar worlds of {} on stop", namespace, e);
        }
    }

    /**
     * The name WorldGuard stores this world's region data under, which is PolarPaper's namespace and the world key.
     */
    public String regionDataName(PolarWorldKey key) {
        return polarNamespace() + "_" + key.value();
    }

    private String polarNamespace() {
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null) throw new IllegalStateException("PolarPaper is not loaded");
        return new NamespacedKey(polarPlugin, "world").getNamespace();
    }

    /**
     * Removes what other plugins keep about worlds of this manager that no longer exist: PolarPaper config entries and WorldGuard region data without a world file. Run once while the plugin enables, before
     * any world is loaded, and logs what it removed.
     *
     * @param owned which worlds the caller considers its own; region data of any other world is left alone
     */
    @Blocking
    public void sweepOrphans(Predicate<PolarWorldKey> owned) {
        try {
            int configEntries = PolarPaperConfig.forget(worldKey -> worldKey.startsWith(prefix));
            int regionData = ServerRegionManager.getInstance().deleteStoredDataWhere(dataName -> {
                PolarWorldKey key = keyOfRegionData(dataName);
                return key != null && owned.test(key) && !Files.exists(source(key).path());
            });
            int dimensionData = deleteDimensionData(dimension -> {
                PolarWorldKey key = keyOf(dimension);
                return key != null && owned.test(key) && !Files.exists(source(key).path());
            });
            RogueSmpCore.LOGGER.info("Orphan sweep of '{}': removed {} stale PolarPaper config entries, {} orphaned WorldGuard data folders and {} orphaned world data folders", namespace, configEntries, regionData, dimensionData);
        } catch (RuntimeException e) {
            RogueSmpCore.LOGGER.error("Orphan sweep of '{}' failed", namespace, e);
        }
    }

    /**
     * Deletes the folders of PolarPaper worlds the filter accepts.
     *
     * @return how many folders were deleted
     */
    @Blocking
    private int deleteDimensionData(Predicate<String> dimensionName) {
        String polarNamespace = polarNamespace();
        int deleted = 0;
        try (Stream<Path> levels = Files.list(Bukkit.getWorldContainer().toPath())) {
            for (Path level : (Iterable<Path>) levels::iterator) {
                Path namespaceFolder = level.resolve("dimensions").resolve(polarNamespace);
                if (!Files.isDirectory(namespaceFolder)) continue;

                try (Stream<Path> dimensions = Files.list(namespaceFolder)) {
                    for (Path dimension : (Iterable<Path>) dimensions::iterator) {
                        if (Files.isDirectory(dimension) && dimensionName.test(dimension.getFileName().toString()) && deleteRecursively(dimension)) deleted++;
                    }
                }
            }
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to scan world data folders of {}", namespace, e);
        }
        return deleted;
    }

    @Blocking
    private static boolean deleteRecursively(Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            RogueSmpCore.LOGGER.warn("Could not fully delete {}, it will be retried by the next orphan sweep", folder, e);
            return false;
        }
    }

    private @Nullable PolarWorldKey keyOfRegionData(String dataName) {
        PolarWorldKey sample = key("sample");
        String sampleName = regionDataName(sample);
        String dataPrefix = sampleName.substring(0, sampleName.length() - sample.name().length());
        if (!dataName.startsWith(dataPrefix)) return null;

        try {
            return key(dataName.substring(dataPrefix.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Blocking
    public int deleteStoredWhere(Predicate<PolarWorldKey> filter) {
        int deleted = 0;
        for (PolarWorldKey key : listStoredKeys()) {
            if (!filter.test(key)) continue;

            try {
                deleteStored(key);
                deleted++;
            } catch (IOException e) {
                RogueSmpCore.LOGGER.error("Failed to delete polar world file {}", key, e);
            }
        }
        return deleted;
    }

    @Blocking
    public void deleteStored(PolarWorldKey key) throws IOException {
        Files.deleteIfExists(source(key).path());
    }

    public CompletableFuture<World> create(PolarWorldKey key, @Nullable PolarWorld base, Consumer<World> initializer) {
        PolarWorld polarWorld = base != null ? base : new PolarWorld(MIN_SECTION, MAX_SECTION);
        return serialized(key, () -> {
            if (getLoaded(key) != null) {
                return CompletableFuture.failedFuture(new IllegalStateException("World already exists: " + key));
            }

            return AsyncUtils.supply(() -> isStored(key)).thenCompose(stored -> {
                if (stored) return CompletableFuture.<World>failedFuture(new IllegalStateException("World already exists: " + key));

                return AsyncUtils.onMainThread(() -> Polar.createWorld(polarWorld, key.value(), polarConfig(key))
                        .thenCompose(world -> AsyncUtils.onMainThread(() -> {
                            try {
                                PolarGenerator.fromWorld(world).setSource(source(key));
                                initializer.accept(world);
                                worldSetup.accept(key, world);
                            } catch (RuntimeException e) {
                                abandon(key, world);
                                throw e;
                            }
                            return Polar.saveWorld(world, source(key)).handle((v, error) -> error)
                                    .thenCompose(error -> AsyncUtils.onMainThread(() -> {
                                        if (error != null) {
                                            abandon(key, world);
                                            return CompletableFuture.<World>failedFuture(error);
                                        }
                                        managed.add(key);
                                        startAutosave(key);
                                        return CompletableFuture.completedFuture(world);
                                    }));
                        })));
            });
        }).whenComplete((world, error) -> {
            if (error != null && !managed.contains(key)) persistence.remove(key);
        });
    }

    public CompletableFuture<World> load(PolarWorldKey key) {
        return serialized(key, () -> doLoad(key));
    }

    public CompletableFuture<Void> save(PolarWorldKey key) {
        return serialized(key, () -> {
            World world = getLoaded(key);
            if (world == null) return CompletableFuture.failedFuture(new IllegalStateException("World is not loaded: " + key));
            if (!isPolar(world)) return CompletableFuture.failedFuture(new IllegalStateException("World is not a Polar world: " + key));
            return Polar.saveWorld(world, source(key));
        });
    }

    public CompletableFuture<Void> unload(PolarWorldKey key, boolean save) {
        return serialized(key, () -> {
            World world = getLoaded(key);
            if (world == null) return CompletableFuture.completedFuture(null);

            return evacuator.evacuate(world)
                    .thenCompose(v -> AsyncUtils.onMainThread(() -> save ? Polar.saveWorld(world, source(key)) : CompletableFuture.<Void>completedFuture(null)))
                    .whenComplete((v, error) -> {
                        if (error != null) RogueSmpCore.LOGGER.error("Failed to save polar world {}, keeping it loaded", key, error);
                    })
                    .thenCompose(v -> AsyncUtils.onMainThread(() -> {
                        if (!Bukkit.unloadWorld(world, false)) {
                            return CompletableFuture.<Void>failedFuture(new IllegalStateException("Could not unload world " + key));
                        }
                        managed.remove(key);
                        stopAutosave(key);
                        return CompletableFuture.completedFuture(null);
                    }));
        });
    }

    public CompletableFuture<Void> discard(World world) {
        PolarWorldKey key = keyOf(world.getKey().getKey());
        if (key == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("World " + world.getKey() + " does not belong to " + namespace));
        }
        return serialized(key, () -> doDiscard(key, world));
    }

    public CompletableFuture<Void> delete(PolarWorldKey key) {
        return serialized(key, () -> {
            NamespacedKey worldKey = namespacedKey(key);
            World world = getLoaded(key);
            CompletableFuture<Void> discarded = world == null ? CompletableFuture.completedFuture(null) : doDiscard(key, world);
            return discarded.thenCompose(v -> AsyncUtils.run(() -> {
                try {
                    deleteStored(key);
                    deleteDimensionData(dimension -> dimension.equals(key.value()));
                    BlockManager.getInstance().deleteWorldState(worldKey);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        });
    }

    /**
     * Copies the stored state of the custom blocks of {@code source} (a world of {@code from}) to {@code target}, before
     * {@code target} loads.
     */
    public CompletableFuture<Void> copyBlockState(PolarWorldManager from, PolarWorldKey source, PolarWorldKey target) {
        return AsyncUtils.onMainThread(() -> BlockManager.getInstance().copyWorldState(from.namespacedKey(source), namespacedKey(target)));
    }

    public CompletableFuture<World> copyFrom(PolarWorldManager from, PolarWorldKey source, PolarWorldKey target) {
        if (!from.owns(source)) return foreign(source);

        Path sourcePath = from.source(source).path();
        Path targetPath = source(target).path();
        NamespacedKey targetKey = namespacedKey(target);
        return serialized(target, () -> {
            if (getLoaded(target) != null) {
                return CompletableFuture.failedFuture(new IllegalStateException("World already exists: " + target));
            }

            return AsyncUtils.run(() -> {
                if (!Files.exists(sourcePath)) throw new IllegalStateException("World file is missing for " + source + " (expected " + sourcePath.getFileName() + ")");
                if (Files.exists(targetPath)) throw new IllegalStateException("World already exists: " + target);
                try {
                    Files.copy(sourcePath, targetPath);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).thenCompose(v -> copyBlockState(from, source, target).thenCompose(copied -> doLoad(target)).exceptionallyCompose(error -> AsyncUtils.run(() -> {
                try {
                    deleteStored(target);
                } catch (IOException e) {
                    RogueSmpCore.LOGGER.warn("Could not remove the partial copy {}, the next orphan sweep may find it", target, e);
                }
                BlockManager.getInstance().deleteWorldState(targetKey);
            }).thenCompose(cleaned -> CompletableFuture.<World>failedFuture(error))));
        });
    }

    private CompletableFuture<World> doLoad(PolarWorldKey key) {
        World loaded = getLoaded(key);
        if (loaded != null) return CompletableFuture.completedFuture(loaded);

        return AsyncUtils.supply(() -> isStored(key))
                .thenCompose(stored -> {
                    if (!stored) return CompletableFuture.<World>failedFuture(new IllegalStateException("World file is missing for " + key));
                    return AsyncUtils.onMainThread(() -> Polar.createWorld(source(key), key.value(), polarConfig(key)));
                })
                .thenCompose(world -> AsyncUtils.onMainThread(() -> {
                    if (world == null) return CompletableFuture.<World>failedFuture(new IllegalStateException("Polar did not create world " + key));
                    try {
                        worldSetup.accept(key, world);
                    } catch (RuntimeException e) {
                        abandon(key, world);
                        throw e;
                    }
                    managed.add(key);
                    startAutosave(key);
                    return CompletableFuture.completedFuture(world);
                }))
                .whenComplete((world, error) -> {
                    if (error != null) {
                        persistence.remove(key);
                        RogueSmpCore.LOGGER.error("Failed to load polar world {}", key, error);
                    }
                });
    }

    private CompletableFuture<Void> doDiscard(PolarWorldKey key, World world) {
        return evacuator.evacuate(world)
                .thenCompose(v -> AsyncUtils.onMainThread(() -> {
                    cancelAutosave(key);
                    return savesInFlight.getOrDefault(key, CompletableFuture.completedFuture(null));
                }))
                .thenCompose(v -> {
                    CompletableFuture<Void> delayed = new CompletableFuture<>();
                    Utils.runLater(() -> delayed.complete(null), EVACUATION_DELAY_TICKS);
                    return delayed;
                })
                .thenCompose(v -> AsyncUtils.onMainThread(() -> {
                    if (!Bukkit.unloadWorld(world, false)) {
                        startAutosave(key);
                        return CompletableFuture.<Void>failedFuture(new IllegalStateException("Could not unload world " + key));
                    }
                    managed.remove(key);
                    persistence.remove(key);
                    return CompletableFuture.completedFuture(null);
                }));
    }

    private void abandon(PolarWorldKey key, World world) {
        Bukkit.unloadWorld(world, false);
        persistence.remove(key);
    }

    /**
     * PolarPaper writes every world it autosaves or saves on stop into its own config file, which never forgets them.
     * So Polar is told to do neither, and this manager autosaves and saves on stop itself.
     */
    private Config polarConfig(PolarWorldKey key) {
        Config requested = configProvider.apply(key);
        persistence.put(key, new Persistence(requested.autoSaveIntervalTicks(), requested.saveOnStop()));
        return requested.toBuilder().autoSaveIntervalTicks(-1).saveOnStop(false).build();
    }

    private void startAutosave(PolarWorldKey key) {
        Persistence settings = persistence.get(key);
        if (settings == null || settings.autoSaveTicks() <= 0) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), () -> autosave(key), settings.autoSaveTicks(), settings.autoSaveTicks());
        BukkitTask previous = autosaveTasks.put(key, task);
        if (previous != null) previous.cancel();
    }

    private void cancelAutosave(PolarWorldKey key) {
        BukkitTask task = autosaveTasks.remove(key);
        if (task != null) task.cancel();
    }

    private void stopAutosave(PolarWorldKey key) {
        cancelAutosave(key);
        persistence.remove(key);
    }

    private void autosave(PolarWorldKey key) {
        World world = getLoaded(key);
        if (world == null) {
            stopAutosave(key);
            return;
        }

        CompletableFuture<Void> done = new CompletableFuture<>();
        if (savesInFlight.putIfAbsent(key, done) != null) return;

        Polar.saveWorld(world, source(key)).whenComplete((v, error) -> {
            savesInFlight.remove(key, done);
            done.complete(null);
            if (error != null) RogueSmpCore.LOGGER.error("Failed to autosave polar world {}", key, error);
        });
    }

    private <T> CompletableFuture<T> foreign(PolarWorldKey key) {
        return CompletableFuture.failedFuture(new IllegalArgumentException("World " + key + " does not belong to the '" + namespace + "' namespace"));
    }

    /**
     * Runs the action on the main thread once every earlier operation on {@code key} has finished, and holds back later
     * ones until the action's future completes. The action must not call another queued operation on the same key.
     */
    public <T> CompletableFuture<T> serialized(PolarWorldKey key, Supplier<CompletableFuture<T>> action) {
        if (!owns(key)) return foreign(key);

        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<?> previous = tails.put(key, result);
        CompletableFuture<?> ready = previous == null ? CompletableFuture.completedFuture(null) : previous.handle((v, error) -> null);

        ready.thenCompose(v -> AsyncUtils.onMainThread(action)).whenComplete((value, error) -> {
            if (error != null) result.completeExceptionally(error);
            else result.complete(value);
            tails.remove(key, result);
        });
        return result;
    }
}
