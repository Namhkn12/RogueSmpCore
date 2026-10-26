package com.roguesmp.dungeon;

import com.roguesmp.codec.DataResult;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.dungeon.feature.MembershipFeature;
import com.roguesmp.dungeon.feature.QuestsFeature;
import com.roguesmp.registry.Registries;
import com.roguesmp.storage.JsonSqliteStore;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.teleport.destination.HubDestination;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class DungeonManager {

    private static final long TICK_INTERVAL_TICKS = 20L;
    private static final int IDLE_UNLOAD_DELAY_TICKS = 20 * 60;
    private static final int MAX_EVENT_DEPTH = 16;
    private static final long INVITE_MILLIS = 60_000L;

    private static DungeonManager INSTANCE;

    private final RogueSmpCore plugin;
    private final DungeonWorldManager worlds;
    private final JsonSqliteStore<DungeonInstance> store;
    private final Map<UUID, DungeonInstance> instances = new ConcurrentHashMap<>();
    private final Set<UUID> ending = ConcurrentHashMap.newKeySet();
    private final Set<String> creating = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<UUID, Invite>> invites = new ConcurrentHashMap<>();
    private final TrackedMobs trackedMobs = new TrackedMobs();

    public record Invite(DungeonInstance instance, UUID inviterId, long expiresAt) {
    }
    private int eventDepth;

    private DungeonManager(RogueSmpCore plugin, DungeonWorldManager worlds) {
        this.plugin = plugin;
        this.worlds = worlds;
        this.store = new JsonSqliteStore<>(plugin, "dungeon/instances.db", "dungeon_instance", "instance_id", DungeonInstance.CODEC);
    }

    public static void init(RogueSmpCore plugin, DungeonWorldManager worlds) {
        INSTANCE = new DungeonManager(plugin, worlds);
    }

    public static DungeonManager getInstance() {
        return INSTANCE;
    }

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

    public boolean isLive(DungeonInstance instance) {
        return instances.get(instance.id()) == instance && !ending.contains(instance.id());
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

    public TrackedMobs trackedMobs() {
        return trackedMobs;
    }

    public DungeonWorldManager worlds() {
        return worlds;
    }

    public void create(Player creator, String dungeonId) {
        DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
        if (definition == null) {
            creator.sendMessage(Utils.fromString("<red>Phó bản này không tồn tại."));
            return;
        }

        Set<UUID> members = new LinkedHashSet<>();
        members.add(creator.getUniqueId());
        for (DungeonFeature<?> feature : definition.features().values()) {
            if (CommandFeedback.failed(creator, feature.prepare(creator, members))) return;
        }

        creator.sendMessage(Utils.fromString("<yellow>Đang tạo phó bản..."));
        CommandFeedback.report(creator, create(dungeonId, creator.getUniqueId(), members).thenCompose(instance -> {
                    members.stream().map(Bukkit::getPlayer).filter(member -> member != null && !member.equals(creator)).forEach(member -> enter(member, instance));
                    return teleportIn(creator, instance);
                }),
                "<green>Đã vào phó bản <yellow>" + definition.displayName() + "<green>.");
    }

    private CompletableFuture<DungeonInstance> create(String dungeonId, UUID creatorId, Set<UUID> members) {
        DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
        if (definition == null) return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này không tồn tại."));

        String pendingKey = creatorId + ":" + dungeonId;
        if (!creating.add(pendingKey)) return CompletableFuture.failedFuture(new IllegalStateException("Phó bản đang được tạo, vui lòng đợi."));

        DungeonInstance instance = new DungeonInstance(dungeonId, definition, worlds.snapshotOf(definition.template()), members);
        for (UUID member : instance.members()) {
            DataResult<Void> free = checkNoInstance(member, dungeonId);
            if (free.isSuccess()) continue;
            creating.remove(pendingKey);
            return CompletableFuture.failedFuture(new IllegalStateException(free.error()));
        }
        return worlds.create(instance.template(), instance.id(), instance.worldData())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> {
                    if (instance.members().stream().anyMatch(member -> findFor(member, dungeonId) != null)) {
                        return discard(instance, new IllegalStateException("Bạn đã có phó bản này rồi."));
                    }
                    instances.put(instance.id(), instance);
                    dispatch(instance, DungeonFeature::onStart);
                    return persist(instance).thenCompose(saved -> {
                        if (!saved) return discardUnsaved(instance);
                        fire(instance, new DungeonEvent.Started());
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

    public void enter(Player player, DungeonInstance instance) {
        CommandFeedback.report(player, teleportIn(player, instance), "<green>Đã vào phó bản <yellow>" + instance.displayName() + "<green>.");
    }

    public void invite(Player inviter, DungeonInstance instance, Player target) {
        if (CommandFeedback.failed(inviter, checkInvite(instance, inviter.getUniqueId(), target.getUniqueId()))) return;
        invites.computeIfAbsent(instance.id(), id -> new ConcurrentHashMap<>())
                .put(target.getUniqueId(), new Invite(instance, inviter.getUniqueId(), System.currentTimeMillis() + INVITE_MILLIS));

        inviter.sendMessage(Utils.fromString("<green>Đã mời <yellow>" + target.getName() + "<green>."));
        target.sendMessage(Utils.fromString("<yellow>" + inviter.getName() + "<green> mời bạn vào phó bản <yellow>" + instance.displayName()
                + "<green>. <yellow><click:run_command:'/dungeonrun join " + instance.dungeonId() + " " + inviter.getName() + "'>[Tham gia]</click>"));
    }

    public void acceptInvite(Player player, DungeonInstance instance) {
        UUID playerId = player.getUniqueId();
        if (!instance.isMember(playerId)) {
            Map<UUID, Invite> pending = invites.get(instance.id());
            Invite invite = pending == null ? null : pending.remove(playerId);
            if (invite == null || invite.expiresAt() < System.currentTimeMillis()) {
                player.sendMessage(Utils.fromString("<red>Bạn chưa được mời hoặc lời mời đã hết hạn."));
                return;
            }
            if (CommandFeedback.failed(player, addMember(instance, playerId))) return;
        }
        enter(player, instance);
    }

    public void abandon(Player player, DungeonInstance instance) {
        UUID playerId = player.getUniqueId();
        if (!allowsAbandon(instance)) {
            player.sendMessage(Utils.fromString("<red>Không thể rời bỏ phó bản này."));
            return;
        }
        if (!instance.isMember(playerId)) {
            player.sendMessage(Utils.fromString("<red>Bạn không phải thành viên của phó bản này."));
            return;
        }
        if (CommandFeedback.failed(player, removeMember(instance, playerId))) return;
        player.sendMessage(Utils.fromString("<yellow>Bạn đã rời bỏ phó bản <white>" + instance.displayName() + "<yellow>."));
    }

    public List<Invite> invitesFor(UUID playerId, String dungeonId) {
        long now = System.currentTimeMillis();
        return invites.values().stream()
                .map(pending -> pending.get(playerId))
                .filter(invite -> invite != null && invite.expiresAt() >= now && invite.instance().dungeonId().equals(dungeonId))
                .toList();
    }

    public boolean allowsInvites(DungeonInstance instance) {
        MembershipFeature membership = membership(instance);
        return membership != null && membership.invites();
    }

    public boolean allowsAbandon(DungeonInstance instance) {
        MembershipFeature membership = membership(instance);
        return membership != null && membership.abandon();
    }

    private CompletableFuture<Void> teleportIn(Player player, DungeonInstance instance) {
        if (!instance.isMember(player.getUniqueId())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bạn không phải thành viên của phó bản này."));
        }
        if (ending.contains(instance.id()) || !instances.containsKey(instance.id())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phó bản này đã kết thúc."));
        }

        return worlds.load(instance.template(), instance.id())
                .thenCompose(world -> AsyncUtils.onMainThread(() -> TeleportService.getInstance()
                        .teleport(player, world.getSpawnLocation(), TeleportOptions.INSTANT).thenAccept(teleported -> { })));
    }

    public void leave(Player player) {
        TeleportService.getInstance().teleport(player, HubDestination.INSTANCE, TeleportOptions.INSTANT);
    }

    public DataResult<Void> addMember(DungeonInstance instance, UUID playerId) {
        if (instance.isMember(playerId)) return DataResult.success(null);
        DataResult<Void> free = checkNoInstance(playerId, instance.dungeonId());
        if (!free.isSuccess()) return free;
        instance.addMember(playerId);
        save(instance);
        return free;
    }

    public DataResult<Void> removeMember(DungeonInstance instance, UUID playerId) {
        if (!instance.isMember(playerId)) return DataResult.error("Người này không phải thành viên của phó bản.");
        instance.removeMember(playerId);
        if (instance.members().isEmpty()) {
            end(instance, "đã bị huỷ vì không còn thành viên");
            return DataResult.success(null);
        }
        save(instance);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && instance.equals(getByWorld(player.getWorld()))) leave(player);
        return DataResult.success(null);
    }

    // Events nested past MAX_EVENT_DEPTH are dropped to break trigger loops.
    public void fire(DungeonInstance instance, DungeonEvent event) {
        if (eventDepth >= MAX_EVENT_DEPTH) {
            RogueSmpCore.LOGGER.warn("Dropped event {} in dungeon run {}: events nested deeper than {}", event, instance.id(), MAX_EVENT_DEPTH);
            return;
        }
        eventDepth++;
        try {
            dispatch(instance, (feature, context) -> feature.onEvent(context, event));
        } finally {
            eventDepth--;
        }
    }

    public CompletableFuture<Void> end(DungeonInstance instance, String reason) {
        if (!instances.containsKey(instance.id()) || !ending.add(instance.id())) return CompletableFuture.completedFuture(null);

        dispatchAll(instance, DungeonFeature::onEnd);
        invites.remove(instance.id());
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
        long now = System.currentTimeMillis();
        invites.values().forEach(pending -> pending.values().removeIf(invite -> invite.expiresAt() < now));
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
        return discard(instance, new IllegalStateException("Không thể lưu phó bản, vui lòng thử lại."));
    }

    private CompletableFuture<DungeonInstance> discard(DungeonInstance instance, Throwable reason) {
        return worlds.delete(instance.template(), instance.id())
                .handle((v, error) -> null)
                .thenCompose(v -> CompletableFuture.<DungeonInstance>failedFuture(reason));
    }

    private DataResult<Void> checkInvite(DungeonInstance instance, UUID inviterId, UUID inviteeId) {
        if (!allowsInvites(instance)) return DataResult.error("Phó bản này không cho phép mời người khác.");
        if (!instance.isMember(inviterId)) return DataResult.error("Chỉ thành viên mới có thể mời người khác.");
        if (instance.isMember(inviteeId)) return DataResult.error("Người này đã ở trong phó bản rồi.");
        return checkNoInstance(inviteeId, instance.dungeonId());
    }

    private @Nullable MembershipFeature membership(DungeonInstance instance) {
        FeatureContext<NoState> context = feature(instance, MembershipFeature.class);
        return context == null ? null : (MembershipFeature) context.feature();
    }

    private DataResult<Void> checkNoInstance(UUID playerId, String dungeonId) {
        if (findFor(playerId, dungeonId) == null) return DataResult.success(null);
        Player player = Bukkit.getPlayer(playerId);
        String name = player != null ? player.getName() : playerId.toString();
        return DataResult.error(name + " đã có phó bản này rồi.");
    }

    private void notifyMembers(DungeonInstance instance, String reason) {
        String name = instance.displayName();
        for (UUID memberId : instance.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) member.sendMessage(Utils.fromString("<yellow>Phó bản <white>" + name + "<yellow> " + reason + "."));
        }
    }
}
