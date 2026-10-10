package com.roguesmp.world;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.AsyncUtils;
import live.minehub.polarpaper.core.config.Config;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Every method is non-blocking and must be called from the main thread, except the constructor path in {@link #init}.
 */
public final class PolarTemplateManager {

    private static final String FOLDER_NAME = "polar_template_worlds";

    private static PolarTemplateManager INSTANCE;

    private final RogueSmpCore plugin;
    private final PolarWorldManager worldManager;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();

    @Blocking
    private PolarTemplateManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.worldManager = new PolarWorldManager(plugin.getDataFolder().toPath().resolve(FOLDER_NAME), this::configFor, this::applyBorder);
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

    public PolarTemplateData data(String name) {
        PolarTemplateData data = Registries.POLAR_TEMPLATE_DATA.get(name);
        return data != null ? data : PolarTemplateData.DEFAULT;
    }

    public @Nullable String nameOf(World world) {
        return worldManager.getManagedName(world);
    }

    public CompletableFuture<World> create(String name, @Nullable String templateName) {
        return exclusive(List.of(name), () -> {
            if (templateName == null) return worldManager.create(name, null, world -> { });

            return worldManager.readStored(templateName).thenCompose(base -> {
                if (base == null) {
                    return CompletableFuture.<World>failedFuture(new IllegalArgumentException("Template world not found: " + templateName));
                }
                return worldManager.create(name, base, world -> { });
            });
        });
    }

    public CompletableFuture<World> load(String name) {
        return exclusive(List.of(name), () -> worldManager.load(name));
    }

    public CompletableFuture<Void> save(String name) {
        return exclusive(List.of(name), () -> worldManager.save(name));
    }

    public CompletableFuture<Void> unload(String name, boolean save) {
        return exclusive(List.of(name), () -> evacuate(worldManager.getLoaded(name)).thenCompose(v -> AsyncUtils.onMainThread(() -> worldManager.unload(name, save))));
    }

    public CompletableFuture<Void> delete(String name) {
        return exclusive(List.of(name), () -> evacuate(worldManager.getLoaded(name))
                .thenCompose(v -> AsyncUtils.onMainThread(() -> worldManager.delete(name)))
                .thenCompose(v -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA.removeAndDeleteFilesAsync(plugin, name)))
                .thenAccept(removed -> { }));
    }

    public CompletableFuture<World> copy(String sourceName, String targetName) {
        return exclusive(List.of(sourceName, targetName), () -> worldManager.copyFrom(worldManager, sourceName, targetName)
                .thenCompose(world -> AsyncUtils.onMainThread(() -> Registries.POLAR_TEMPLATE_DATA
                        .registerAndSaveAsync(plugin, targetName, data(sourceName))
                        .thenApply(saved -> world))));
    }

    public CompletableFuture<Void> setSpawn(String name, Location spawn) {
        World world = worldManager.getLoaded(name);
        if (world != null) {
            Location located = spawn.clone();
            located.setWorld(world);
            world.setSpawnLocation(located);
        }
        return Registries.POLAR_TEMPLATE_DATA.registerAndSaveAsync(plugin, name, data(name).withSpawn(spawn)).thenAccept(saved -> { });
    }

    public CompletableFuture<Void> setBorder(String name, double diameter) {
        return worldManager.exists(name).thenCompose(exists -> {
            if (!exists) return CompletableFuture.<Void>failedFuture(new IllegalArgumentException("Unknown template world: " + name));

            return AsyncUtils.onMainThread(() -> {
                CompletableFuture<Void> saved = Registries.POLAR_TEMPLATE_DATA
                        .registerAndSaveAsync(plugin, name, data(name).withBorderDiameter(diameter))
                        .thenAccept(data -> { });

                World world = worldManager.getLoaded(name);
                if (world == null) return saved;
                if (diameter <= 0) world.getWorldBorder().reset();
                else applyBorder(world);
                return saved;
            });
        });
    }

    private Config configFor(String name) {
        return Config.Builder.defaults()
                .spawn(data(name).spawn())
                .loadOnStartup(false)
                .saveOnStop(false)
                .autoSaveIntervalTicks(-1)
                .announceAutosave(false)
                .build();
    }

    private void applyBorder(World world) {
        double diameter = data(world.getKey().getKey()).borderDiameter();
        if (diameter <= 0) return;
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(diameter);
    }

    private CompletableFuture<Void> evacuate(@Nullable World world) {
        if (world == null || world.getPlayers().isEmpty()) return CompletableFuture.completedFuture(null);

        LoadedMultiverseWorld hub = MultiverseCoreApi.get().getWorldManager().getDefaultWorld().getOrNull();
        if (hub == null) return CompletableFuture.failedFuture(new IllegalStateException("No default world to evacuate players to"));

        Location hubSpawn = hub.getSpawnLocation();
        List<CompletableFuture<Boolean>> teleports = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            teleports.add(player.teleportAsync(hubSpawn));
        }

        return CompletableFuture.allOf(teleports.toArray(CompletableFuture[]::new))
                .thenCompose(v -> AsyncUtils.onMainThread(() -> world.getPlayers().isEmpty()
                        ? CompletableFuture.<Void>completedFuture(null)
                        : CompletableFuture.<Void>failedFuture(new IllegalStateException("Could not evacuate every player from " + world.getName()))));
    }

    private <T> CompletableFuture<T> exclusive(List<String> names, Supplier<CompletableFuture<T>> action) {
        List<String> acquired = new ArrayList<>();
        for (String name : names) {
            if (!busy.add(name)) {
                acquired.forEach(busy::remove);
                return CompletableFuture.failedFuture(new IllegalStateException("Another operation is already running on world " + name));
            }
            acquired.add(name);
        }

        CompletableFuture<T> result;
        try {
            result = action.get();
        } catch (RuntimeException e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.whenComplete((value, error) -> acquired.forEach(busy::remove));
    }
}
