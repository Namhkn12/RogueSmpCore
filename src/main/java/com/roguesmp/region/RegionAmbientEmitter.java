package com.roguesmp.region;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.flag.AmbientParticles;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RegionAmbientEmitter {

    private static final long TICK_INTERVAL = 10L;
    private static final int REFRESH_EVERY = 10;
    private static final int POINT_ATTEMPTS = 8;

    private record Emitter(World world, AmbientParticles particles, List<ProtectedRegion> pieces, long[] cumulativeVolume,
                           ServerRegionManager.Bounds bounds) {
    }

    private List<Emitter> emitters = List.of();
    private int ticksUntilRefresh;

    private RegionAmbientEmitter(RogueSmpCore plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public static void init(RogueSmpCore plugin) {
        new RegionAmbientEmitter(plugin);
    }

    private void refresh() {
        ServerRegionManager manager = ServerRegionManager.getInstance();
        List<Emitter> found = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().isEmpty()) continue;
            for (Map.Entry<String, List<ProtectedRegion>> entry : manager.regions(world).entrySet()) {
                ProtectedRegion settings = manager.get(world, entry.getKey());
                AmbientParticles particles = settings == null ? null : settings.getFlag(SmpRegionFlags.AMBIENT_PARTICLES);
                ServerRegionManager.Bounds bounds = ServerRegionManager.boundsOf(entry.getValue());
                if (particles == null || bounds == null) continue;

                List<ProtectedRegion> pieces = List.copyOf(entry.getValue());
                long[] cumulative = new long[pieces.size()];
                long total = 0;
                for (int i = 0; i < pieces.size(); i++) cumulative[i] = total += Math.max(1, pieces.get(i).volume());
                found.add(new Emitter(world, particles, pieces, cumulative, bounds));
            }
        }
        emitters = found;
        ticksUntilRefresh = REFRESH_EVERY;
    }

    private void tick() {
        if (--ticksUntilRefresh <= 0) refresh();

        for (Emitter emitter : emitters) {
            if (Utils.RANDOM.nextDouble() >= emitter.particles().chance()) continue;

            List<Player> viewers = viewers(emitter);
            if (viewers.isEmpty()) continue;

            for (int i = 0; i < emitter.particles().count(); i++) emitAtRandomPoint(emitter, viewers);
        }
    }

    private List<Player> viewers(Emitter emitter) {
        double view = emitter.particles().view();
        double viewSquared = view * view;
        BlockVector3 min = emitter.bounds().min();
        BlockVector3 max = emitter.bounds().max();

        List<Player> viewers = new ArrayList<>();
        for (Player player : emitter.world().getPlayers()) {
            Location location = player.getLocation();
            double dx = distanceOutside(location.getX(), min.x(), max.x() + 1);
            double dy = distanceOutside(location.getY(), min.y(), max.y() + 1);
            double dz = distanceOutside(location.getZ(), min.z(), max.z() + 1);
            if (dx * dx + dy * dy + dz * dz <= viewSquared) viewers.add(player);
        }
        return viewers;
    }

    private static double distanceOutside(double value, double min, double max) {
        return value < min ? min - value : value > max ? value - max : 0;
    }

    private void emitAtRandomPoint(Emitter emitter, List<Player> viewers) {
        long[] cumulative = emitter.cumulativeVolume();
        long roll = Utils.RANDOM.nextLong(cumulative[cumulative.length - 1]);
        int index = 0;
        while (cumulative[index] <= roll) index++;
        ProtectedRegion piece = emitter.pieces().get(index);

        BlockVector3 min = piece.getMinimumPoint();
        BlockVector3 max = piece.getMaximumPoint();
        for (int attempt = 0; attempt < POINT_ATTEMPTS; attempt++) {
            double x = min.x() + Utils.RANDOM.nextDouble() * (max.x() - min.x() + 1);
            double y = min.y() + Utils.RANDOM.nextDouble() * (max.y() - min.y() + 1);
            double z = min.z() + Utils.RANDOM.nextDouble() * (max.z() - min.z() + 1);
            if (!piece.contains((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) continue;

            for (Player viewer : viewers) viewer.spawnParticle(emitter.particles().particle(), x, y, z, 1, 0, 0, 0, 0);
            return;
        }
    }
}
