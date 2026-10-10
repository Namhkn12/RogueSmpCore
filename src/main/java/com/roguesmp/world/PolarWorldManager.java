package com.roguesmp.world;

import com.roguesmp.RogueSmpCore;
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
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Owns every {@code .polar} world file in one folder. World names share PolarPaper's global namespace, so callers
 * must keep names unique across managers.
 * <p>
 * Every method that returns a future is non-blocking and safe to call from the main thread. Methods annotated
 * {@link Blocking} do file IO on the calling thread and must only be called off the main thread.
 */
public final class PolarWorldManager {

    private static final String EXTENSION = ".polar";
    private static final byte MIN_SECTION = -4;
    private static final byte MAX_SECTION = 19;
    private static final int EVACUATION_DELAY_TICKS = 20;

    private final Path folder;
    private final Function<String, Config> configProvider;
    private final Consumer<World> worldSetup;
    private final Set<String> managed = ConcurrentHashMap.newKeySet();
    private final Map<String, CompletableFuture<World>> loading = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> unloading = new ConcurrentHashMap<>();

    /**
     * @param configProvider builds the Polar config (spawn, autosave, ...) for a world name, each time it is created or loaded
     * @param worldSetup     applied on the main thread to every world right after it is created or loaded (e.g. world border)
     */
    @Blocking
    public PolarWorldManager(Path folder, Function<String, Config> configProvider, Consumer<World> worldSetup) {
        this.folder = folder;
        this.configProvider = configProvider;
        this.worldSetup = worldSetup;
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public FilePolarSource source(String name) {
        return new FilePolarSource(folder.resolve(name + EXTENSION));
    }

    public @Nullable World getLoaded(String name) {
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null) return null;
        return Bukkit.getWorld(new NamespacedKey(polarPlugin, name));
    }

    /**
     * @return the name of the loaded world if this manager created or loaded it, without touching the disk
     */
    public @Nullable String getManagedName(World world) {
        String name = world.getKey().getKey();
        return managed.contains(name) && world.equals(getLoaded(name)) ? name : null;
    }

    public CompletableFuture<Boolean> exists(String name) {
        if (getLoaded(name) != null) return CompletableFuture.completedFuture(true);
        return AsyncUtils.supply(() -> isStored(name));
    }

    public CompletableFuture<List<String>> storedNames() {
        return AsyncUtils.supply(this::listStoredNames);
    }

    public CompletableFuture<@Nullable PolarWorld> readStored(String name) {
        return AsyncUtils.supply(() -> readStoredFile(name));
    }

    @Blocking
    private boolean isStored(String name) {
        return Files.exists(source(name).path());
    }

    @Blocking
    private List<String> listStoredNames() {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(EXTENSION))
                    .map(fileName -> fileName.substring(0, fileName.length() - EXTENSION.length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to list polar worlds in {}", folder, e);
            return List.of();
        }
    }

    @Blocking
    private @Nullable PolarWorld readStoredFile(String name) {
        if (!isStored(name)) return null;
        try {
            return new PolarReader().read(source(name));
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to read polar world {}", name, e);
            return null;
        }
    }

    @Blocking
    public void deleteStored(String name) throws IOException {
        Files.deleteIfExists(source(name).path());
    }

    public CompletableFuture<World> create(String name, @Nullable PolarWorld base, Consumer<World> initializer) {
        if (getLoaded(name) != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("World already exists: " + name));
        }

        PolarWorld polarWorld = base != null ? base : new PolarWorld(MIN_SECTION, MAX_SECTION);
        return AsyncUtils.supply(() -> isStored(name)).thenCompose(stored -> {
            if (stored) return CompletableFuture.failedFuture(new IllegalStateException("World already exists: " + name));
            return AsyncUtils.onMainThread(() -> Polar.createWorld(polarWorld, name, configProvider.apply(name))
                    .thenCompose(world -> {
                        CompletableFuture<Void> prepared = new CompletableFuture<>();
                        Utils.runLater(() -> {
                            try {
                                PolarGenerator.fromWorld(world).setSource(source(name));
                                initializer.accept(world);
                                worldSetup.accept(world);
                                prepared.complete(null);
                            } catch (RuntimeException e) {
                                prepared.completeExceptionally(e);
                            }
                        });
                        return prepared.thenCompose(v -> Polar.saveWorld(world, source(name))).thenApply(v -> world);
                    }));
        }).thenApply(world -> {
            managed.add(name);
            return world;
        });
    }

    public CompletableFuture<World> load(String name) {
        return AsyncUtils.onMainThread(() -> {
            CompletableFuture<Void> pendingUnload = unloading.get(name);
            if (pendingUnload != null) return pendingUnload.handle((v, error) -> null).thenCompose(v -> load(name));

            World loaded = getLoaded(name);
            if (loaded != null) return CompletableFuture.completedFuture(loaded);

            CompletableFuture<World> inFlight = loading.get(name);
            if (inFlight != null) return inFlight;

            CompletableFuture<World> future = AsyncUtils.supply(() -> isStored(name)).thenCompose(stored -> {
                if (!stored) return CompletableFuture.failedFuture(new IllegalStateException("World file is missing for " + name));
                return AsyncUtils.onMainThread(() -> Polar.createWorld(source(name), name, configProvider.apply(name)));
            });
            loading.put(name, future);
            future.whenComplete((world, error) -> Utils.runLater(() -> {
                loading.remove(name);
                if (error != null) RogueSmpCore.LOGGER.error("Failed to load polar world {}", name, error);
                else if (world == null) RogueSmpCore.LOGGER.error("Polar world {} was not created by Polar", name);
                else {
                    managed.add(name);
                    worldSetup.accept(world);
                }
            }));
            return future;
        });
    }

    public CompletableFuture<Void> save(String name) {
        World world = getLoaded(name);
        if (world == null) return CompletableFuture.failedFuture(new IllegalStateException("World is not loaded: " + name));
        return Polar.saveWorld(world, source(name));
    }

    public CompletableFuture<Void> unload(String name, boolean save) {
        World world = getLoaded(name);
        if (world == null) return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> pending = new CompletableFuture<>();
        CompletableFuture<Void> existing = unloading.putIfAbsent(name, pending);
        if (existing != null) return existing;

        CompletableFuture<Void> saved = save ? Polar.saveWorld(world, source(name)) : CompletableFuture.completedFuture(null);
        saved.whenComplete((v, error) -> Utils.runLater(() -> {
            unloading.remove(name);
            if (error != null) {
                RogueSmpCore.LOGGER.error("Failed to save polar world {}, keeping it loaded", name, error);
                pending.completeExceptionally(error);
                return;
            }
            Bukkit.unloadWorld(world, false);
            managed.remove(name);
            pending.complete(null);
        }));
        return pending;
    }

    public CompletableFuture<Void> discard(World world) {
        String name = world.getKey().getKey();
        CompletableFuture<Void> pendingUnload = unloading.get(name);
        if (pendingUnload != null) return pendingUnload;

        Polar.stopAutoSaveTask(world.getKey());

        CompletableFuture<Void> discarded = new CompletableFuture<>();
        Utils.runLater(() -> {
            Bukkit.unloadWorld(world, false);
            managed.remove(name);
            discarded.complete(null);
        }, EVACUATION_DELAY_TICKS);
        return discarded;
    }

    public CompletableFuture<Void> delete(String name) {
        World world = getLoaded(name);
        CompletableFuture<Void> discarded = world == null ? CompletableFuture.completedFuture(null) : discard(world);
        return discarded.thenCompose(v -> AsyncUtils.run(() -> {
            try {
                deleteStored(name);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    /**
     * Copies the saved file of a world, possibly from another manager's folder, and completes with the loaded copy.
     */
    public CompletableFuture<World> copyFrom(PolarWorldManager from, String sourceName, String targetName) {
        if (getLoaded(targetName) != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("World already exists: " + targetName));
        }

        Path sourcePath = from.source(sourceName).path();
        Path targetPath = source(targetName).path();
        return AsyncUtils.run(() -> {
            if (!Files.exists(sourcePath)) throw new IllegalStateException("World file is missing for " + sourceName);
            if (Files.exists(targetPath)) throw new IllegalStateException("World already exists: " + targetName);
            try {
                Files.copy(sourcePath, targetPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).thenCompose(v -> load(targetName));
    }
}
