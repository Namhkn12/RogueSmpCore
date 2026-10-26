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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RegionAmbientEmitter {

    private static final long TICK_INTERVAL = 10L;
    private static final int REFRESH_EVERY = 10;
    private static final int POINT_ATTEMPTS = 8;

    private record Emitter(World world, AmbientParticles particles, List<ProtectedRegion> pieces, long[] cumulativeVolume,
                           ServerRegionManager.@Nullable Bounds bounds) {

        static Emitter global(World world, AmbientParticles particles) {
            return new Emitter(world, particles, List.of(), new long[0], null);
        }

        boolean isGlobal() {
            return bounds == null;
        }

        double nearRadius() {
            return isGlobal() ? particles.view() : particles.near();
        }
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

            ProtectedRegion global = manager.global(world);
            AmbientParticles globalParticles = global == null ? null : global.getFlag(SmpRegionFlags.AMBIENT_PARTICLES);
            if (globalParticles != null) found.add(Emitter.global(world, globalParticles));

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
            AmbientParticles particles = emitter.particles();
            List<Player> outside = new ArrayList<>();
            List<Player> inside = new ArrayList<>();
            sortViewers(emitter, inside, outside);

            if (!outside.isEmpty() && Utils.RANDOM.nextDouble() < particles.chance()) {
                for (int i = 0; i < particles.count(); i++) emitAtRandomPoint(emitter, outside);
            }

            for (Player player : inside) {
                if (Utils.RANDOM.nextDouble() >= particles.chance()) continue;
                for (int i = 0; i < particles.count(); i++) emitNear(emitter, player);
            }
        }
    }

    private void sortViewers(Emitter emitter, List<Player> inside, List<Player> outside) {
        if (emitter.isGlobal()) {
            inside.addAll(emitter.world().getPlayers());
            return;
        }

        double view = emitter.particles().view();
        double viewSquared = view * view;
        BlockVector3 min = emitter.bounds().min();
        BlockVector3 max = emitter.bounds().max();

        for (Player player : emitter.world().getPlayers()) {
            Location location = player.getLocation();
            double dx = distanceOutside(location.getX(), min.x(), max.x() + 1);
            double dy = distanceOutside(location.getY(), min.y(), max.y() + 1);
            double dz = distanceOutside(location.getZ(), min.z(), max.z() + 1);
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > viewSquared) continue;

            boolean within = distanceSquared == 0 && contains(emitter, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            (within ? inside : outside).add(player);
        }
    }

    private static boolean contains(Emitter emitter, int x, int y, int z) {
        if (emitter.isGlobal()) return true;
        for (ProtectedRegion piece : emitter.pieces()) {
            if (piece.contains(x, y, z)) return true;
        }
        return false;
    }

    private void emitNear(Emitter emitter, Player player) {
        double near = emitter.nearRadius();
        Location center = player.getLocation();
        for (int attempt = 0; attempt < POINT_ATTEMPTS; attempt++) {
            double ox = (Utils.RANDOM.nextDouble() * 2 - 1) * near;
            double oy = (Utils.RANDOM.nextDouble() * 2 - 1) * near;
            double oz = (Utils.RANDOM.nextDouble() * 2 - 1) * near;
            if (ox * ox + oy * oy + oz * oz > near * near) continue;

            double x = center.getX() + ox;
            double y = center.getY() + oy;
            double z = center.getZ() + oz;
            if (!contains(emitter, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))) continue;

            player.spawnParticle(emitter.particles().particle(), x, y, z, 1, 0, 0, 0, 0);
            return;
        }
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
