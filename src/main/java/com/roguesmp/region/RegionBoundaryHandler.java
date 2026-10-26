package com.roguesmp.region;

import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.event.RegionLeaveEvent;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class RegionBoundaryHandler extends Handler {

    private static final Factory<RegionBoundaryHandler> FACTORY = new Factory<>() {
        @Override
        public RegionBoundaryHandler create(Session session) {
            return new RegionBoundaryHandler(session);
        }
    };

    private @Nullable World occupiedWorld;
    private Set<String> occupied = Set.of();

    private RegionBoundaryHandler(Session session) {
        super(session);
    }

    public static void register() {
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(FACTORY, null);
    }

    public static void unregister() {
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(FACTORY);
    }

    @Override
    public void initialize(LocalPlayer localPlayer, Location current, ApplicableRegionSet set) {
        Player player = Bukkit.getPlayer(localPlayer.getUniqueId());
        if (player == null) return;

        World world = BukkitAdapter.adapt((com.sk89q.worldedit.world.World) current.getExtent());
        Utils.runLater(() -> {
            if (occupiedWorld == null && player.isOnline()) transition(player, world, set);
        });
    }

    @Override
    public void uninitialize(LocalPlayer localPlayer, Location current, ApplicableRegionSet set) {
        occupiedWorld = null;
        occupied = Set.of();
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer localPlayer, Location from, Location to, ApplicableRegionSet toSet,
                                   Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        Player player = Bukkit.getPlayer(localPlayer.getUniqueId());
        if (player == null) return true;

        transition(player, BukkitAdapter.adapt((com.sk89q.worldedit.world.World) to.getExtent()), toSet);
        return true;
    }

    private void transition(Player player, World world, ApplicableRegionSet regions) {
        Set<String> now = new HashSet<>();
        for (ProtectedRegion region : regions) now.add(ServerRegionManager.logicalId(region));

        boolean sameWorld = world.equals(occupiedWorld);
        Set<String> left = new HashSet<>(occupied);
        Set<String> entered = new HashSet<>(now);
        if (sameWorld) {
            left.removeAll(now);
            entered.removeAll(occupied);
        }

        World leftWorld = occupiedWorld;
        occupiedWorld = world;
        occupied = now;

        for (String regionId : left) new RegionLeaveEvent(player, leftWorld, regionId, regions).callEvent();
        for (String regionId : entered) new RegionEnterEvent(player, world, regionId, regions).callEvent();
    }
}
