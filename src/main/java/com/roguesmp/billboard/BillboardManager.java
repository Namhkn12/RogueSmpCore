package com.roguesmp.billboard;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.billboard.line.TextLine;
import com.roguesmp.codec.DataResult;
import com.roguesmp.constant.Keys;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.AsyncUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class BillboardManager {

    private static final int CLICK_COOLDOWN_TICKS = 5;

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_/-]{1,64}");

    private static BillboardManager INSTANCE;

    private final RogueSmpCore plugin;
    private final Map<String, CompletableFuture<Void>> writes = new ConcurrentHashMap<>();

    private final Map<UUID, Placed> placed = new HashMap<>();
    private final List<BillboardInstance> temporary = new ArrayList<>();
    private final Map<Integer, BillboardInstance> byHitboxId = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final BukkitTask task;
    private long tick;

    public record Placed(UUID markerId, String id, BillboardInstance instance) {
    }

    private BillboardManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new BillboardManager(plugin);
    }

    public static BillboardManager getInstance() {
        return INSTANCE;
    }

    public DataResult<Billboard> create(String id, Location location) {
        if (!VALID_ID.matcher(id).matches()) {
            return DataResult.error("Invalid billboard id. Use lowercase letters, digits, _, - or /.");
        }
        if (Registries.BILLBOARD.get(id) != null) return DataResult.error("Billboard '" + id + "' already exists.");

        Billboard billboard = Billboard.of(List.of(TextLine.of("<yellow>" + id)));
        store(id, billboard);
        sweepLoaded(id);
        place(id, location);
        return DataResult.success(billboard);
    }

    public DataResult<Billboard> update(String id, UnaryOperator<Billboard> change) {
        Billboard current = Registries.BILLBOARD.get(id);
        if (current == null) return DataResult.error("Unknown billboard '" + id + "'.");

        Billboard updated = change.apply(current);
        if (updated.refresh() <= 0) return DataResult.error("Refresh must be at least 1 tick.");
        if (updated.viewRange() <= 0) return DataResult.error("View range must be positive.");

        store(id, updated);
        return DataResult.success(updated);
    }

    public boolean delete(String id) {
        if (!Registries.BILLBOARD.unregister(id)) return false;
        removeMarkers(id);
        enqueueWrite(id, () -> Registries.BILLBOARD.get(id) != null
                ? CompletableFuture.completedFuture(null)
                : Registries.BILLBOARD.removeAndDeleteFilesAsync(plugin, id).thenAccept(deleted -> { }));
        return true;
    }

    private void store(String id, Billboard billboard) {
        Registries.BILLBOARD.register(id, billboard);
        respawn(id);
        enqueueWrite(id, () -> {
            Billboard latest = Registries.BILLBOARD.get(id);
            return latest == null
                    ? CompletableFuture.completedFuture(null)
                    : Registries.BILLBOARD.registerAndSaveAsync(plugin, id, latest).thenAccept(saved -> { });
        });
    }

    public List<Placed> copiesOf(String id) {
        return placed.values().stream().filter(entry -> entry.id().equals(id)).toList();
    }

    private void enqueueWrite(String id, Supplier<CompletableFuture<Void>> write) {
        CompletableFuture<Void> previous = writes.getOrDefault(id, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> next = previous.exceptionally(error -> null).thenCompose(ignored -> AsyncUtils.onMainThread(write));
        writes.put(id, next);
        next.whenComplete((ignored, error) -> {
            if (error != null) RogueSmpCore.LOGGER.error("Failed to write billboard '{}'", id, error);
            writes.remove(id, next);
        });
    }

    private void removeMarkers(String id) {
        placed.entrySet().stream()
                .filter(entry -> entry.getValue().id().equals(id))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(markerId -> {
                    if (Bukkit.getEntity(markerId) instanceof Marker marker) marker.remove();
                    detach(markerId);
                });
    }

    public void reload() {
        List.copyOf(placed.keySet()).forEach(this::detach);
        sweepLoaded(null);
    }

    private void respawn(String id) {
        placed.entrySet().stream()
                .filter(entry -> entry.getValue().id().equals(id))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::reattach);
    }

    private void reattach(UUID markerId) {
        if (Bukkit.getEntity(markerId) instanceof Marker marker && marker.isValid()) attach(marker);
        else detach(markerId);
    }

    public Marker place(String id, Location location) {
        return location.getWorld().spawn(location, Marker.class,
                marker -> marker.getPersistentDataContainer().set(Keys.BILLBOARD_ID, PersistentDataType.STRING, id));
    }

    public static @Nullable String placedId(Entity entity) {
        if (!(entity instanceof Marker)) return null;
        return entity.getPersistentDataContainer().get(Keys.BILLBOARD_ID, PersistentDataType.STRING);
    }

    public static @Nullable Marker nearestPlaced(Location location, double radius, @Nullable String id) {
        return location.getNearbyEntitiesByType(Marker.class, radius).stream()
                .filter(marker -> {
                    String placedId = placedId(marker);
                    return placedId != null && (id == null || id.equals(placedId));
                })
                .min(Comparator.comparingDouble(marker -> marker.getLocation().distanceSquared(location)))
                .orElse(null);
    }

    public void nudge(Marker marker, double x, double y, double z) {
        marker.teleport(marker.getLocation().add(x, y, z));
        attach(marker);
    }

    void attach(Marker marker) {
        String id = placedId(marker);
        if (id == null) return;
        detach(marker.getUniqueId());

        Billboard billboard = Registries.BILLBOARD.get(id);
        if (billboard == null) {
            RogueSmpCore.LOGGER.warn("Placed billboard at {} references unknown billboard '{}'", marker.getLocation(), id);
            return;
        }
        placed.put(marker.getUniqueId(), new Placed(marker.getUniqueId(), id, track(new BillboardInstance(billboard, marker.getLocation(), null))));
    }

    void detach(UUID markerId) {
        Placed previous = placed.remove(markerId);
        if (previous != null) untrack(previous.instance());
    }

    private void sweepLoaded(@Nullable String id) {
        for (World world : Bukkit.getWorlds()) {
            for (Marker marker : world.getEntitiesByClass(Marker.class)) {
                String placedId = placedId(marker);
                if (placedId != null && (id == null || id.equals(placedId))) attach(marker);
            }
        }
    }

    public BillboardInstance spawnTemporary(Billboard billboard, Location anchor) {
        return spawnTemporary(billboard, anchor, null);
    }

    public BillboardInstance spawnTemporary(Billboard billboard, Location anchor, @Nullable BiConsumer<Player, Boolean> onClick) {
        BillboardInstance instance = track(new BillboardInstance(billboard, anchor, onClick));
        temporary.add(instance);
        return instance;
    }

    public void remove(BillboardInstance instance) {
        untrack(instance);
        temporary.remove(instance);
        placed.values().removeIf(entry -> entry.instance() == instance);
    }

    public Map<UUID, Placed> getPlaced() {
        return Collections.unmodifiableMap(placed);
    }

    void click(Player player, int entityId, boolean rightClick) {
        BillboardInstance instance = byHitboxId.get(entityId);
        if (instance == null) return;
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && tick - last < CLICK_COOLDOWN_TICKS) return;
        lastClick.put(player.getUniqueId(), tick);
        instance.click(player, rightClick);
    }

    void forget(UUID uuid) {
        forEachInstance(instance -> instance.forget(uuid));
        lastClick.remove(uuid);
    }

    private void forEachInstance(Consumer<BillboardInstance> action) {
        placed.values().forEach(entry -> action.accept(entry.instance()));
        temporary.forEach(action);
    }

    private BillboardInstance track(BillboardInstance instance) {
        int hitboxId = instance.getHitboxEntityId();
        if (hitboxId >= 0) byHitboxId.put(hitboxId, instance);
        return instance;
    }

    private void untrack(BillboardInstance instance) {
        instance.destroy();
        byHitboxId.remove(instance.getHitboxEntityId());
    }

    private void tick() {
        tick++;
        forEachInstance(BillboardInstance::tick);
    }

    public void shutdown() {
        task.cancel();
        forEachInstance(BillboardInstance::destroy);
        placed.clear();
        temporary.clear();
        byHitboxId.clear();
    }
}
