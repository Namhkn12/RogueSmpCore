package com.roguesmp.fishing;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FishingHotspotManager {

    public record ActiveHotspot(String hotspotId, FishingHotspot hotspot, World world, String regionId, long expiresAtMillis) { }

    private static final long TICK_INTERVAL = 10L;
    private static final long SPAWN_INTERVAL_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_ACTIVE = 2;
    private static final double AMBIENT_RADIUS = 28;
    private static final double AMBIENT_Y_VARIANCE = 3.0;
    private static final double Y_EDGE_MARGIN = 0.01;
    private static final int AMBIENT_POINTS = 8;
    private static final int AMBIENT_SAMPLE_ATTEMPTS = 30;

    private static FishingHotspotManager INSTANCE;

    private final Map<String, ActiveHotspot> activeByRegion = new HashMap<>();
    private final Set<UUID> notifiedHooks = new HashSet<>();
    private long nextSpawnAtMillis = System.currentTimeMillis() + SPAWN_INTERVAL_MILLIS;

    private FishingHotspotManager(RogueSmpCore plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new FishingHotspotManager(plugin);
    }

    public static FishingHotspotManager getInstance() {
        return INSTANCE;
    }

    public @Nullable ActiveHotspot findAt(Location location) {
        if (activeByRegion.isEmpty()) return null;

        for (String regionId : ServerRegionManager.getInstance().regionIdsAt(location)) {
            ActiveHotspot active = activeByRegion.get(regionKey(location.getWorld(), regionId));
            if (active != null) return active;
        }
        return null;
    }

    public @Nullable ActiveHotspot getActive(World world, String regionId) {
        return activeByRegion.get(regionKey(world, regionId));
    }

    private static String regionKey(World world, String regionId) {
        return world.getKey().asString() + "/" + regionId;
    }

    public boolean activate(String hotspotId, World world, String regionId) {
        FishingHotspot hotspot = Registries.FISHING_HOTSPOT.get(hotspotId);
        if (hotspot == null || ServerRegionManager.getInstance().get(world, regionId) == null) return false;

        String key = regionKey(world, regionId);
        if (activeByRegion.containsKey(key)) return false;

        long expiresAt = System.currentTimeMillis() + hotspot.lifetimeSeconds() * 1000L;
        ActiveHotspot active = new ActiveHotspot(hotspotId, hotspot, world, regionId, expiresAt);
        activeByRegion.put(key, active);

        world.getPlayers().forEach(player -> player.sendMessage(FishingHotspotMessages.appeared(active)));
        List<ProtectedRegion> shapes = ServerRegionManager.getInstance().pieces(world, regionId);
        for (Player player : world.getPlayers()) {
            Location location = player.getLocation();
            if (ServerRegionManager.contains(shapes, location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                FishingHotspotMessages.sendEntered(player, active);
            }
        }
        return true;
    }

    public void clear() {
        activeByRegion.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        activeByRegion.values().removeIf(active -> active.expiresAtMillis() <= now);

        if (now >= nextSpawnAtMillis) {
            nextSpawnAtMillis = now + SPAWN_INTERVAL_MILLIS;
            if (activeByRegion.size() < MAX_ACTIVE) spawnRandom();
        }

        if (activeByRegion.isEmpty()) {
            notifiedHooks.clear();
            return;
        }

        activeByRegion.values().forEach(this::showAmbient);

        Set<UUID> hooksInHotspot = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            FishHook hook = player.getFishHook();
            if (hook != null && hook.isValid() && affectHook(player, hook)) hooksInHotspot.add(hook.getUniqueId());
        }
        notifiedHooks.retainAll(hooksInHotspot);
    }

    private void showAmbient(ActiveHotspot active) {
        World world = active.world();
        List<ProtectedRegion> shapes = ServerRegionManager.getInstance().pieces(world, active.regionId());
        ServerRegionManager.Bounds bounds = ServerRegionManager.boundsOf(shapes);
        if (bounds == null) return;

        for (Player viewer : world.getPlayers()) showAmbientTo(viewer, shapes, bounds);
    }

    private void showAmbientTo(Player viewer, List<ProtectedRegion> shapes, ServerRegionManager.Bounds bounds) {
        Location location = viewer.getLocation();
        if (distanceToSquared(bounds, location) > AMBIENT_RADIUS * AMBIENT_RADIUS) return;

        int minX = Math.max(bounds.min().x(), (int) Math.floor(location.getX() - AMBIENT_RADIUS));
        int maxX = Math.min(bounds.max().x(), (int) Math.floor(location.getX() + AMBIENT_RADIUS));
        int minZ = Math.max(bounds.min().z(), (int) Math.floor(location.getZ() - AMBIENT_RADIUS));
        int maxZ = Math.min(bounds.max().z(), (int) Math.floor(location.getZ() + AMBIENT_RADIUS));

        Random random = Utils.RANDOM;
        double headY = viewer.getEyeLocation().getY();
        double minY = bounds.min().y();
        double maxY = bounds.max().y() + 1 - Y_EDGE_MARGIN;
        int found = 0;
        for (int attempt = 0; attempt < AMBIENT_SAMPLE_ATTEMPTS && found < AMBIENT_POINTS; attempt++) {
            int x = random.nextInt(minX, maxX + 1);
            int z = random.nextInt(minZ, maxZ + 1);
            double y = Math.max(minY, Math.min(maxY, headY + random.nextDouble(-AMBIENT_Y_VARIANCE, AMBIENT_Y_VARIANCE)));
            if (!ServerRegionManager.contains(shapes, x, (int) Math.floor(y), z)) continue;

            found++;
            FishingEffects.ambient(viewer, new Location(viewer.getWorld(), x + random.nextDouble(), y, z + random.nextDouble()));
        }
    }

    private double distanceToSquared(ServerRegionManager.Bounds bounds, Location location) {
        double dx = Math.max(Math.max(bounds.min().x() - location.getX(), 0), location.getX() - (bounds.max().x() + 1));
        double dz = Math.max(Math.max(bounds.min().z() - location.getZ(), 0), location.getZ() - (bounds.max().z() + 1));
        return dx * dx + dz * dz;
    }

    private boolean affectHook(Player player, FishHook hook) {
        if (!hook.isInWater()) return false;
        ActiveHotspot active = findAt(hook.getLocation());
        if (active == null) return false;

        player.sendActionBar(Utils.fromString("<aqua>Điểm câu: ").append(Utils.fromString(active.hotspot().displayName())));
        FishingEffects.hotspotPulse(hook.getLocation());
        if (notifiedHooks.add(hook.getUniqueId())) FishingEffects.hotspotEnter(player, hook.getLocation());
        return true;
    }

    private void spawnRandom() {
        Map<String, List<HotspotRegion>> free = new HashMap<>();
        Registries.FISHING_HOTSPOT.getAll().forEach((id, hotspot) -> {
            if (hotspot.weight() <= 0) return;

            List<HotspotRegion> available = new ArrayList<>();
            for (HotspotRegion ref : hotspot.regions()) {
                if (ref.resolveWorld() != null && !activeByRegion.containsKey(ref.key())) available.add(ref);
            }
            if (!available.isEmpty()) free.put(id, available);
        });

        String hotspotId = rollHotspotId(free.keySet());
        if (hotspotId == null) return;

        List<HotspotRegion> candidates = free.get(hotspotId);
        Collections.shuffle(candidates);
        for (HotspotRegion ref : candidates) {
            if (activate(hotspotId, ref.resolveWorld(), ref.region())) return;
        }
    }

    private @Nullable String rollHotspotId(Set<String> ids) {
        int totalWeight = ids.stream().mapToInt(id -> Registries.FISHING_HOTSPOT.getOrThrow(id).weight()).sum();
        if (totalWeight <= 0) return null;

        int roll = Utils.RANDOM.nextInt(totalWeight);
        for (String id : ids) {
            roll -= Registries.FISHING_HOTSPOT.getOrThrow(id).weight();
            if (roll < 0) return id;
        }
        return null;
    }
}
