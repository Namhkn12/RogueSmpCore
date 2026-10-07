package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;

public class IslandRegionManager {
    private static final String REGION_ID = "island";
    private static final int WORLD_PADDING = 5;

    public void createIslandRegion(World world, UUID creatorId) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) {
            RogueSmpCore.LOGGER.error("WorldGuard region manager could not be resolved for world: {}", world.getName());
            return;
        }

        int radius = IslandWorldManager.BORDER_DIAMETER / 2;
        BlockVector3 minPoint = BlockVector3.at(-radius, world.getMinHeight() - WORLD_PADDING, -radius);
        BlockVector3 maxPoint = BlockVector3.at(radius, world.getMaxHeight() + WORLD_PADDING, radius);

        ProtectedRegion region = new ProtectedCuboidRegion(REGION_ID, minPoint, maxPoint);
        DefaultDomain members = region.getMembers();
        members.addPlayer(creatorId);
        region.setMembers(members);

        region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);

        region.setFlag(Flags.BUILD.getRegionGroupFlag(), RegionGroup.MEMBERS);
        region.setFlag(Flags.CHEST_ACCESS.getRegionGroupFlag(), RegionGroup.MEMBERS);
        region.setFlag(Flags.INTERACT.getRegionGroupFlag(), RegionGroup.MEMBERS);
        regionManager.addRegion(region);
        saveAsync(regionManager);
    }

    public void addTeammate(World world, UUID teammateId) {
        editMembers(world, members -> members.addPlayer(teammateId));
    }

    public void removeTeammate(World world, UUID teammateId) {
        editMembers(world, members -> members.removePlayer(teammateId));
    }

    private void editMembers(World world, Consumer<DefaultDomain> edit) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return;

        ProtectedRegion region = regionManager.getRegion(REGION_ID);
        if (region == null) return;

        DefaultDomain members = region.getMembers();
        edit.accept(members);
        region.setMembers(members);
        saveAsync(regionManager);
    }

    public void removeIslandRegion(World world) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager != null) regionManager.removeRegion(REGION_ID);
    }

    public void deleteStoredRegionData(String worldName) {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null) return;

        Path worldFolder = worldGuard.getDataFolder().toPath().resolve("worlds").resolve(worldName);
        if (!Files.exists(worldFolder)) return;

        try (var paths = Files.walk(worldFolder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to delete WorldGuard data for world {}", worldName, e);
        }
    }

    public void saveNow(World world) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return;

        try {
            regionManager.saveChanges();
        } catch (StorageException e) {
            RogueSmpCore.LOGGER.error("Failed to save WorldGuard regions of {}", world.getName(), e);
        }
    }

    private void saveAsync(RegionManager regionManager) {
        Utils.runAsync(() -> {
            try {
                regionManager.saveChanges();
            } catch (StorageException e) {
                RogueSmpCore.LOGGER.error("Failed to save WorldGuard regions", e);
            }
        });
    }

    private @Nullable RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }
}
