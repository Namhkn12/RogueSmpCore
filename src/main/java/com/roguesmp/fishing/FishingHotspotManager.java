package com.roguesmp.fishing;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class FishingHotspotManager {

    public record ActiveHotspot(String hotspotId, FishingHotspot hotspot, Location center, long expiresAtMillis) { }

    private static final long TICK_INTERVAL = 10L;
    private static final long SPAWN_INTERVAL_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_ACTIVE = 2;
    private static final int RING_POINTS = 24;

    private static FishingHotspotManager INSTANCE;

    private final Map<String, ActiveHotspot> activeBySpot = new HashMap<>();
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
        for (ActiveHotspot active : activeBySpot.values()) {
            if (!active.center().getWorld().equals(location.getWorld())) continue;

            double dx = active.center().getX() - location.getX();
            double dz = active.center().getZ() - location.getZ();
            double radius = active.hotspot().radius();
            if (dx * dx + dz * dz <= radius * radius) return active;
        }
        return null;
    }

    public boolean activate(String spotId, String hotspotId, Location center) {
        FishingHotspot hotspot = Registries.FISHING_HOTSPOT.get(hotspotId);
        if (hotspot == null) return false;

        long expiresAt = System.currentTimeMillis() + hotspot.lifetimeSeconds() * 1000L;
        activeBySpot.put(spotId, new ActiveHotspot(hotspotId, hotspot, center, expiresAt));
        Bukkit.broadcast(Utils.fromString("<aqua><bold>[Fishing]</bold> <gray>A hotspot appeared: ").append(Utils.fromString(hotspot.displayName())));
        return true;
    }

    public void clear() {
        activeBySpot.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        activeBySpot.values().removeIf(active -> active.expiresAtMillis() <= now);

        if (now >= nextSpawnAtMillis) {
            nextSpawnAtMillis = now + SPAWN_INTERVAL_MILLIS;
            if (activeBySpot.size() < MAX_ACTIVE) spawnRandom();
        }

        activeBySpot.values().forEach(this::showParticles);
    }

    private void spawnRandom() {
        List<Map.Entry<String, FishingHotspotSpot>> freeSpots = new ArrayList<>();
        for (Map.Entry<String, FishingHotspotSpot> spot : Registries.FISHING_HOTSPOT_SPOT.getAll().entrySet()) {
            if (!activeBySpot.containsKey(spot.getKey()) && spot.getValue().position().getWorld() != null) freeSpots.add(spot);
        }
        if (freeSpots.isEmpty()) return;

        Map.Entry<String, FishingHotspotSpot> spot = freeSpots.get(ThreadLocalRandom.current().nextInt(freeSpots.size()));
        String hotspotId = rollHotspotId(spot.getValue());
        if (hotspotId == null) return;

        activate(spot.getKey(), hotspotId, spot.getValue().position().toLocation());
    }

    private @Nullable String rollHotspotId(FishingHotspotSpot spot) {
        Map<String, FishingHotspot> all = new HashMap<>(Registries.FISHING_HOTSPOT.getAll());
        all.keySet().removeIf(id -> !spot.allows(id));
        int totalWeight = all.values().stream().mapToInt(FishingHotspot::weight).filter(weight -> weight > 0).sum();
        if (totalWeight <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Map.Entry<String, FishingHotspot> entry : all.entrySet()) {
            if (entry.getValue().weight() <= 0) continue;
            roll -= entry.getValue().weight();
            if (roll < 0) return entry.getKey();
        }
        return null;
    }

    private void showParticles(ActiveHotspot active) {
        Location center = active.center();
        double radius = active.hotspot().radius();

        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    center.getX() + Math.cos(angle) * radius, center.getY() + 0.2, center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
        center.getWorld().spawnParticle(Particle.FISHING, center.getX(), center.getY() + 0.1, center.getZ(), 6, radius / 3, 0, radius / 3, 0);
    }
}
