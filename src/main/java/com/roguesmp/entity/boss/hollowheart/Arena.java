package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Arena {

    private static final double FALLBACK_RADIUS = 12.0;
    private static final int FLOOR_SCAN_UP = 3;
    private static final int FLOOR_SCAN_DOWN = 6;
    private static final int POINT_ATTEMPTS = 12;

    private final Location center;
    private final List<ProtectedRegion> shapes;

    private Arena(Location center, List<ProtectedRegion> shapes) {
        this.center = center;
        this.shapes = shapes;
    }

    public static Arena resolve(Location center, String regionId) {
        List<ProtectedRegion> shapes = ServerRegionManager.getInstance().pieces(center.getWorld(), regionId);
        if (shapes.isEmpty()) {
            RogueSmpCore.LOGGER.warn("Arena region '{}' not found in world '{}', falling back to a {} block radius",
                    regionId, center.getWorld().getName(), FALLBACK_RADIUS);
        }
        return new Arena(center.clone(), List.copyOf(shapes));
    }

    public Location center() {
        return center.clone();
    }

    public double radius() {
        if (shapes.isEmpty()) return FALLBACK_RADIUS;
        ServerRegionManager.Bounds bounds = ServerRegionManager.boundsOf(shapes);
        double dx = Math.max(Math.abs(bounds.min().x() - center.getX()), Math.abs(bounds.max().x() + 1 - center.getX()));
        double dz = Math.max(Math.abs(bounds.min().z() - center.getZ()), Math.abs(bounds.max().z() + 1 - center.getZ()));
        return Math.max(dx, dz);
    }

    public boolean contains(Location location) {
        if (location.getWorld() != center.getWorld()) return false;
        if (shapes.isEmpty()) return location.distanceSquared(center) <= FALLBACK_RADIUS * FALLBACK_RADIUS;
        return ServerRegionManager.contains(shapes, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public List<Player> players() {
        List<Player> players = new ArrayList<>();
        for (Player player : center.getWorld().getPlayers()) {
            if (!player.isValid() || player.isDead()) continue;
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) continue;
            if (contains(player.getLocation())) players.add(player);
        }
        return players;
    }

    public List<Location> ringPoints(int count, double minDistance, double maxDistance) {
        List<Location> points = new ArrayList<>();
        double baseAngle = Utils.RANDOM.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + Math.PI * 2 * i / count;
            Location point = pointNear(angle, minDistance, maxDistance);
            if (point != null) points.add(point);
        }
        return points;
    }

    public @Nullable Location randomPoint(double maxDistance) {
        return pointNear(Utils.RANDOM.nextDouble() * Math.PI * 2, 0, maxDistance);
    }

    private @Nullable Location pointNear(double angle, double minDistance, double maxDistance) {
        for (int attempt = 0; attempt < POINT_ATTEMPTS; attempt++) {
            double jitteredAngle = angle + (attempt == 0 ? 0 : (Utils.RANDOM.nextDouble() - 0.5) * 0.8);
            double distance = minDistance + Utils.RANDOM.nextDouble() * (maxDistance - minDistance);
            Location floor = floorAt(center.getX() + Math.cos(jitteredAngle) * distance, center.getZ() + Math.sin(jitteredAngle) * distance);
            if (floor != null) return floor;
        }
        return null;
    }

    public @Nullable Location floorAt(double x, double z) {
        Location probe = new Location(center.getWorld(), x, center.getBlockY() + FLOOR_SCAN_UP, z);
        for (int dy = 0; dy <= FLOOR_SCAN_UP + FLOOR_SCAN_DOWN; dy++) {
            Block block = probe.getBlock().getRelative(0, -dy, 0);
            if (!block.getType().isSolid()) continue;
            Block feet = block.getRelative(0, 1, 0);
            if (!feet.isPassable() || !feet.getRelative(0, 1, 0).isPassable()) continue;
            Location standing = feet.getLocation().add(0.5, 0, 0.5);
            return contains(standing) ? standing : null;
        }
        return null;
    }
}
