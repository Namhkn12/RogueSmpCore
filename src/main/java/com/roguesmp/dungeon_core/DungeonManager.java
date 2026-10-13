package com.roguesmp.dungeon_core;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon_core.feature.QuestsFeature;
import com.roguesmp.registry.Registries;
import com.roguesmp.storage.JsonSqliteStore;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.world.Hub;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Owns every dungeon run: creates its world from the dungeon's template, routes gameplay to its features and destroys
 * it when it ends. Methods that change a run must be called from the main thread.
 */
public final class DungeonManager {

    private static final long TICK_INTERVAL_TICKS = 20L;
    private static final int IDLE_UNLOAD_DELAY_TICKS = 20 * 60;
    private static final int MAX_SIGNAL_DEPTH = 16;

    private static DungeonManager INSTANCE;

    private final RogueSmpCore plugin;
    private final DungeonWorldManager worlds;
    private final JsonSqliteStore<DungeonInstance> store;
    private final Map<UUID, DungeonInstance> instances = new ConcurrentHashMap<>();
    private final Set<UUID> ending = ConcurrentHashMap.newKeySet();
    private final Set<String> creating = ConcurrentHashMap.newKeySet();
    private int signalDepth;

    private DungeonManager(RogueSmpCore plugin, DungeonWorldManager worlds) {
        this.plugin = plugin;
        this.worlds = worlds;
        this.store = new JsonSqliteStore<>(plugin, "dungeon_instance.db", "dungeon_instance", "instance_id", DungeonInstance.CODEC);
    }

    public static void init(RogueSmpCore plugin, DungeonWorldManager worlds) {
        INSTANCE = new DungeonManager(plugin, worlds);
    }

    public static DungeonManager getInstance() {
        return INSTANCE;
    }

    /**
     * Startup only, after dungeon definitions and quests are loaded: restores saved runs, deletes worlds and quest groups
     * left by runs that no longer exist, then starts ticking.
     */
    @Blocking
    public void load() {
        instances.putAll(store.loadAll());
        for (DungeonInstance instance : instances.values()) {
            if (instance.worldData() != null) worlds.remember(instance.id(), instance.worldData());
            DungeonDefinition definition = Registries.DUNGEON.get(instance.dungeonId());
            if (definition == null) {
                RogueSmpCore.LOGGER.warn("Dungeon run {} refers to unknown dungeon {}, it stays inactive", instance.id(), instance.dungeonId());
                continue;
            }
            instance.bind(definition);
        }

        Set<UUID> recordedIds = store.tryLoadKeys();
        if (recordedIds == null) {
            RogueSmpCore.LOGGER.warn("Skipping the dungeon run orphan sweep: run records could not be read");
        } else {
            RogueSmpCore.LOGGER.info("Orphan sweep of dungeon run worlds: deleted {} world(s) without a run record", worlds.deleteOrphans(recordedIds));
            QuestsFeature.sweep(recordedIds);
        }

        instances.values().forEach(instance -> dispatch(instance, DungeonFeature::onRestore));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL_TICKS, TICK_INTERVAL_TICKS);
    }

    public Collection<DungeonInstance> instances() {
        return instances.values();
    }

    public @Nullable DungeonInstance get(UUID instanceId) {
        return instances.get(instanceId);
    }

    public @Nullable DungeonInstance getByWorld(World world) {
        UUID instanceId = worlds.getInstanceId(world);
        return instanceId == null ? null : instances.get(instanceId);
    }

    public @Nullable World worldOf(DungeonInstance instance) {
        return worlds.getLoaded(instance.template(), instance.id());
    }

    public DungeonWorldManager worlds() {
        return worlds;
    }

    /**
     * Clones the template, runs the features' start hooks, saves the run, then sends {@link DungeonSignal.Started}.
     * Rules about who may create a run belong to the caller.
     */
    public CompletableFuture<DungeonInstance> create(String dungeonId, UUID ownerId, Set<UUID> members) {
        DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
        if (definition == null) return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này không tồn tại."));

        String pendingKey = ownerId + ":" + dungeonId;
        if (!creating.add(pendingKey)) return CompletableFuture.failedFuture(new IllegalStateException("Phó bản đang được tạo, vui lòng đợi."));

        DungeonInstance instance = new DungeonInstance(dungeonId, definition, worlds.snapshotOf(definition.template()), ownerId, members);
        return worlds.create(instance.template(), instance.id(), instance.worldData())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> {
                    instances.put(instance.id(), instance);
                    dispatch(instance, DungeonFeature::onStart);
                    return persist(instance).thenCompose(saved -> {
                        if (!saved) return discardUnsaved(instance);
                        signal(instance, new DungeonSignal.Started());
                        return CompletableFuture.completedFuture(instance);
                    });
                }))
                .whenComplete((created, error) -> creating.remove(pendingKey));
    }

    public @Nullable DungeonInstance findFor(UUID playerId, String dungeonId) {
        return instances.values().stream()
                .filter(instance -> instance.dungeonId().equals(dungeonId) && instance.isMember(playerId))
                .findFirst()
                .orElse(null);
    }

    public List<DungeonInstance> instancesOf(UUID playerId) {
        return instances.values().stream().filter(instance -> instance.isMember(playerId)).toList();
    }

    /**
     * @return the context of the run's first feature of that type, or null if its dungeon has none
     */
    @SuppressWarnings("unchecked")
    public <S, F extends DungeonFeature<S>> @Nullable FeatureContext<S> feature(DungeonInstance instance, Class<F> type) {
        DungeonDefinition definition = instance.definition();
        if (definition == null) return null;
        for (Map.Entry<String, DungeonFeature<?>> entry : definition.features().entrySet()) {
            if (type.isInstance(entry.getValue())) {
                return new FeatureContext<>(this, instance, entry.getKey(), (DungeonFeature<S>) entry.getValue(), instance.state(entry.getKey()));
            }
        }
        return null;
    }

    public CompletableFuture<Void> enter(Player player, DungeonInstance instance) {
        if (!instance.isMember(player.getUniqueId())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bạn không phải thành viên của phó bản này."));
        }
        if (ending.contains(instance.id()) || !instances.containsKey(instance.id())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này đã kết thúc."));
        }

        return worlds.load(instance.template(), instance.id())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> player.teleportAsync(world.getSpawnLocation()).thenAccept(teleported -> { })));
    }

    public void leave(Player player) {
        player.teleportAsync(Hub.spawn());
    }

    public void addMember(DungeonInstance instance, UUID playerId) {
        instance.addMember(playerId);
        save(instance);
    }

    public void removeMember(DungeonInstance instance, UUID playerId) {
        if (instance.ownerId().equals(playerId)) throw new IllegalStateException("Không thể gỡ chủ phó bản, hãy kết thúc phó bản.");
        instance.removeMember(playerId);
        save(instance);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && instance.equals(getByWorld(player.getWorld()))) leave(player);
    }

    /**
     * Delivers the signal to every feature of the run. Signals sent while handling one are nested; past
     * {@link #MAX_SIGNAL_DEPTH} levels they are dropped, which stops a trigger that answers its own signal.
     */
    public void signal(DungeonInstance instance, DungeonSignal signal) {
        if (signalDepth >= MAX_SIGNAL_DEPTH) {
            RogueSmpCore.LOGGER.warn("Dropped signal {} in dungeon run {}: signals nested deeper than {}", signal, instance.id(), MAX_SIGNAL_DEPTH);
            return;
        }
        signalDepth++;
        try {
            dispatch(instance, (feature, context) -> feature.onSignal(context, signal));
        } finally {
            signalDepth--;
        }
    }

    /**
     * Runs the features' end hooks, sends everyone inside back to the hub, then deletes the world and the record.
     */
    public CompletableFuture<Void> end(DungeonInstance instance, String reason) {
        if (!instances.containsKey(instance.id()) || !ending.add(instance.id())) return CompletableFuture.completedFuture(null);

        dispatchAll(instance, DungeonFeature::onEnd);
        World world = worldOf(instance);
        if (world != null) world.getPlayers().forEach(this::leave);
        notifyMembers(instance, reason);

        return worlds.delete(instance.template(), instance.id())
                .thenCompose(v -> store.deleteAsync(instance.id()))
                .whenComplete((v, error) -> {
                    instances.remove(instance.id());
                    ending.remove(instance.id());
                    if (error != null) RogueSmpCore.LOGGER.error("Failed to destroy dungeon run {}", instance.id(), error);
                });
    }

    public void save(DungeonInstance instance) {
        persist(instance);
    }

    public void unloadIfIdleLater(World world) {
        DungeonInstance instance = getByWorld(world);
        if (instance == null) return;

        Utils.runLater(() -> {
            World loaded = worldOf(instance);
            if (loaded == null || !loaded.getPlayers().isEmpty()) return;
            if (!instances.containsKey(instance.id()) || ending.contains(instance.id())) return;

            save(instance);
            worlds.unload(instance.template(), instance.id()).exceptionally(error -> {
                RogueSmpCore.LOGGER.error("Failed to unload idle dungeon run {}", instance.id(), error);
                return null;
            });
        }, IDLE_UNLOAD_DELAY_TICKS);
    }

    /**
     * Calls {@code hook} on every feature of the run with its own context. A failing feature is logged and does not stop
     * the others. Runs that are not bound to a definition or are ending are skipped.
     */
    public void dispatch(DungeonInstance instance, BiConsumer<DungeonFeature<Object>, FeatureContext<Object>> hook) {
        if (!ending.contains(instance.id())) dispatchAll(instance, hook);
    }

    @SuppressWarnings("unchecked")
    private void dispatchAll(DungeonInstance instance, BiConsumer<DungeonFeature<Object>, FeatureContext<Object>> hook) {
        DungeonDefinition definition = instance.definition();
        if (definition == null) return;

        definition.features().forEach((featureId, feature) -> {
            try {
                hook.accept((DungeonFeature<Object>) feature, new FeatureContext<>(this, instance, featureId, (DungeonFeature<Object>) feature, instance.state(featureId)));
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Feature {} of dungeon {} failed in run {}", featureId, instance.dungeonId(), instance.id(), e);
            }
        });
    }

    @Blocking
    public void onDisable() {
        for (DungeonInstance instance : instances.values()) {
            String json = store.encode(instance);
            if (json != null) store.write(instance.id(), json);
        }
        worlds.saveAllOnStop();
        store.close();
    }

    private void tick() {
        for (DungeonInstance instance : List.copyOf(instances.values())) {
            dispatch(instance, DungeonFeature::onTick);
        }
    }

    private CompletableFuture<Boolean> persist(DungeonInstance instance) {
        String json = store.encode(instance);
        return json == null ? CompletableFuture.completedFuture(false) : store.writeAsync(instance.id(), json);
    }

    private CompletableFuture<DungeonInstance> discardUnsaved(DungeonInstance instance) {
        instances.remove(instance.id());
        return worlds.delete(instance.template(), instance.id())
                .handle((v, error) -> null)
                .thenCompose(v -> CompletableFuture.<DungeonInstance>failedFuture(new IllegalStateException("Không thể lưu phó bản, vui lòng thử lại.")));
    }

    private void notifyMembers(DungeonInstance instance, String reason) {
        DungeonDefinition definition = instance.definition();
        String name = definition != null ? definition.displayName() : instance.dungeonId();
        for (UUID memberId : instance.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) member.sendMessage(Utils.fromString("<yellow>Phó bản <white>" + name + "<yellow> " + reason + "."));
        }
    }
}
