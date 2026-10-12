package com.roguesmp.dungeon_static;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import com.roguesmp.storage.JsonSqliteStore;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns every static dungeon instance. A player can belong to one instance per dungeon, members cannot be removed
 * by the owner once joined, and an instance is destroyed (world, world data, record) when it expires or on request.
 * <p>
 * Methods that change instance state must be called from the main thread. Rule violations throw
 * {@link IllegalStateException} with a player-facing message.
 */
public final class StaticDungeonManager {

    private static final long IDLE_UNLOAD_DELAY_TICKS = 20L * 60;
    private static final long EXPIRY_CHECK_INTERVAL_TICKS = 20L * 30;
    private static final long INVITE_TTL_MILLIS = 60_000;

    private static StaticDungeonManager INSTANCE;

    private record Invite(UUID inviteeId, UUID instanceId) {
    }

    private final DungeonWorldManager worlds;
    private final JsonSqliteStore<StaticDungeonInstance> store;
    private final Map<UUID, StaticDungeonInstance> instances = new ConcurrentHashMap<>();
    private final Map<Invite, Long> invites = new HashMap<>();
    private final Set<UUID> destroying = ConcurrentHashMap.newKeySet();
    private final Set<String> creating = ConcurrentHashMap.newKeySet();

    @Blocking
    private StaticDungeonManager(RogueSmpCore plugin, DungeonWorldManager worlds) {
        this.worlds = worlds;
        this.store = new JsonSqliteStore<>(plugin, "static_dungeon.db", "static_dungeon", "instance_id", StaticDungeonInstance.CODEC);
        this.instances.putAll(store.loadAll());
        instances.values().forEach(instance -> {
            if (instance.worldData() != null) worlds.remember(instance.id(), instance.worldData());
        });

        Set<UUID> recordedIds = store.tryLoadKeys();
        if (recordedIds == null) {
            RogueSmpCore.LOGGER.warn("Skipping the dungeon orphan sweep: instance records could not be read");
        } else {
            RogueSmpCore.LOGGER.info("Orphan sweep of dungeon worlds: deleted {} world(s) without an instance record", worlds.deleteOrphans(recordedIds));
        }

        Bukkit.getScheduler().runTaskTimer(plugin, this::expireDue, EXPIRY_CHECK_INTERVAL_TICKS, EXPIRY_CHECK_INTERVAL_TICKS);
    }

    @Blocking
    public static void init(RogueSmpCore plugin, DungeonWorldManager worlds) {
        INSTANCE = new StaticDungeonManager(plugin, worlds);
        Utils.runLater(INSTANCE::expireDue);
    }

    public static StaticDungeonManager getInstance() {
        return INSTANCE;
    }

    public DungeonWorldManager worlds() {
        return worlds;
    }

    public Collection<StaticDungeonInstance> instances() {
        return instances.values();
    }

    public @Nullable StaticDungeonInstance get(UUID instanceId) {
        return instances.get(instanceId);
    }

    public @Nullable StaticDungeonInstance getByWorld(World world) {
        UUID instanceId = worlds.getInstanceId(world);
        return instanceId == null ? null : instances.get(instanceId);
    }

    public @Nullable StaticDungeonInstance findFor(UUID playerId, String dungeonId) {
        return instances.values().stream()
                .filter(instance -> instance.dungeonId().equals(dungeonId) && instance.isMember(playerId))
                .findFirst()
                .orElse(null);
    }

    public @Nullable StaticDungeonInstance findOwnedBy(UUID ownerId, String dungeonId) {
        return instances.values().stream()
                .filter(instance -> instance.dungeonId().equals(dungeonId) && instance.ownerId().equals(ownerId))
                .findFirst()
                .orElse(null);
    }

    public List<StaticDungeonInstance> instancesOf(UUID playerId) {
        return instances.values().stream().filter(instance -> instance.isMember(playerId)).toList();
    }

    public CompletableFuture<StaticDungeonInstance> create(UUID ownerId, String dungeonId) {
        StaticDungeonInfo info = Registries.STATIC_DUNGEON_INFO.get(dungeonId);
        if (info == null) return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này không tồn tại."));

        if (findFor(ownerId, dungeonId) != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bạn đã có phó bản này rồi."));
        }

        String pendingKey = ownerId + ":" + dungeonId;
        if (!creating.add(pendingKey)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phó bản đang được tạo, vui lòng đợi."));
        }

        StaticDungeonInstance instance = new StaticDungeonInstance(ownerId, dungeonId, info.template(), worlds.snapshotOf(info.template()), info.expiry().expiresAt(DateUtils.now()));
        return worlds.create(info.template(), instance.id(), instance.worldData())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> {
                    instances.put(instance.id(), instance);
                    return persist(instance).thenCompose(saved -> saved
                            ? CompletableFuture.completedFuture(instance)
                            : discardUnsaved(instance));
                }))
                .whenComplete((created, error) -> creating.remove(pendingKey));
    }

    public void invite(StaticDungeonInstance instance, UUID inviterId, UUID inviteeId) {
        if (!instance.ownerId().equals(inviterId)) throw new IllegalStateException("Chỉ chủ phó bản mới có thể mời người khác.");
        if (instance.isMember(inviteeId)) throw new IllegalStateException("Người này đã ở trong phó bản rồi.");
        requireNoInstance(inviteeId, instance.dungeonId());

        invites.put(new Invite(inviteeId, instance.id()), System.currentTimeMillis() + INVITE_TTL_MILLIS);
    }

    public StaticDungeonInstance acceptInvite(UUID inviteeId, UUID ownerId, String dungeonId) {
        StaticDungeonInstance instance = findOwnedBy(ownerId, dungeonId);
        if (instance == null) throw new IllegalStateException("Không tìm thấy phó bản này.");
        if (instance.isMember(inviteeId)) return instance;

        Long expiresAt = invites.remove(new Invite(inviteeId, instance.id()));
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            throw new IllegalStateException("Bạn chưa được mời hoặc lời mời đã hết hạn.");
        }

        addMember(instance, inviteeId);
        return instance;
    }

    public void addMember(StaticDungeonInstance instance, UUID playerId) {
        requireNoInstance(playerId, instance.dungeonId());
        instance.addMember(playerId);
        save(instance);
    }

    public void removeMember(StaticDungeonInstance instance, UUID playerId) {
        if (instance.ownerId().equals(playerId)) throw new IllegalStateException("Không thể gỡ chủ phó bản, hãy huỷ phó bản.");
        instance.removeMember(playerId);
        save(instance);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && instance.equals(getByWorld(player.getWorld()))) exit(player);
    }

    public void extend(StaticDungeonInstance instance, long expiresAt) {
        instance.setExpiresAt(expiresAt);
        save(instance);
    }

    public CompletableFuture<Void> enter(Player player, StaticDungeonInstance instance) {
        if (!instance.isMember(player.getUniqueId())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bạn không phải thành viên của phó bản này."));
        }
        if (instance.isExpired(System.currentTimeMillis())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này đã hết hạn."));
        }
        if (destroying.contains(instance.id())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này đang bị huỷ."));
        }

        return worlds.load(instance.template(), instance.id())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> player.teleportAsync(world.getSpawnLocation()).thenAccept(teleported -> { })));
    }

    public void exit(Player player) {
        MultiverseCoreApi.get().getWorldManager().getDefaultWorld().peek(hub -> player.teleportAsync(hub.getSpawnLocation()));
    }

    public CompletableFuture<Void> destroy(StaticDungeonInstance instance, String reason) {
        if (!destroying.add(instance.id())) return CompletableFuture.completedFuture(null);

        return worlds.delete(instance.template(), instance.id())
                .thenCompose(v -> AsyncUtils.run(() -> store.delete(instance.id())))
                .thenAccept(v -> {
                    instances.remove(instance.id());
                    notifyMembers(instance, reason);
                })
                .whenComplete((v, error) -> {
                    destroying.remove(instance.id());
                    if (error != null) RogueSmpCore.LOGGER.error("Failed to destroy static dungeon {}", instance.id(), error);
                });
    }

    public void expireDue() {
        long now = System.currentTimeMillis();
        invites.values().removeIf(expiresAt -> expiresAt < now);

        for (StaticDungeonInstance instance : List.copyOf(instances.values())) {
            if (instance.isExpired(now)) destroy(instance, "đã hết hạn");
        }
    }

    public void unloadIfIdleLater(World world) {
        StaticDungeonInstance instance = getByWorld(world);
        if (instance == null) return;

        Utils.runLater(() -> {
            World loaded = worlds.getLoaded(instance.template(), instance.id());
            if (loaded == null || !loaded.getPlayers().isEmpty()) return;
            if (!instances.containsKey(instance.id()) || destroying.contains(instance.id())) return;

            worlds.unload(instance.template(), instance.id()).exceptionally(error -> {
                RogueSmpCore.LOGGER.error("Failed to unload idle static dungeon {}", instance.id(), error);
                return null;
            });
        }, (int) IDLE_UNLOAD_DELAY_TICKS);
    }

    @Blocking
    public void onDisable() {
        worlds.saveAllOnStop();
        store.close();
    }

    private void requireNoInstance(UUID playerId, String dungeonId) {
        if (findFor(playerId, dungeonId) != null) throw new IllegalStateException("Người chơi này đã có phó bản " + dungeonId + " rồi.");
    }

    private void save(StaticDungeonInstance instance) {
        persist(instance);
    }

    private CompletableFuture<Boolean> persist(StaticDungeonInstance instance) {
        String json = store.encode(instance);
        return json == null ? CompletableFuture.completedFuture(false) : store.writeAsync(instance.id(), json);
    }

    private CompletableFuture<StaticDungeonInstance> discardUnsaved(StaticDungeonInstance instance) {
        instances.remove(instance.id());
        return worlds.delete(instance.template(), instance.id())
                .handle((v, error) -> null)
                .thenCompose(v -> CompletableFuture.<StaticDungeonInstance>failedFuture(new IllegalStateException("Không thể lưu phó bản, vui lòng thử lại.")));
    }

    private void notifyMembers(StaticDungeonInstance instance, String reason) {
        for (UUID memberId : instance.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) member.sendMessage(Utils.fromString("<yellow>Phó bản <white>" + StaticDungeonInfo.displayNameOf(instance.dungeonId()) + "<yellow> " + reason + "."));
        }
    }
}
