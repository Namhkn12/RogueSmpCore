package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class IslandRegionManager {
    private static final int BORDER_DIAMETER = 250;
    private static final String REGION_PREFIX = "island_";

    /**
     * Provisions a precise WorldGuard cuboid region spanning from sky-limit to bedrock-floor
     * centered around the newly generated island space.
     */
    public void createIslandRegion(Player creator, Location islandCenter, UUID islandId) {
        World world = islandCenter.getWorld();
        if (world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            RogueSmpCore.LOGGER.error("WorldGuard region manager could not be resolved for world: {}", world.getName());
            return;
        }

        int radius = BORDER_DIAMETER / 2;

        BlockVector3 minPoint = BlockVector3.at(
                islandCenter.getBlockX() - radius,
                world.getMinHeight() - 5, //Extra 5 for leniency
                islandCenter.getBlockZ() - radius
        );

        BlockVector3 maxPoint = BlockVector3.at(
                islandCenter.getBlockX() + radius,
                world.getMaxHeight() + 5,
                islandCenter.getBlockZ() + radius
        );

        String regionName = getRegionName(islandId); //Essentially island_<uuid>
        ProtectedRegion region = new ProtectedCuboidRegion(regionName, minPoint, maxPoint);
        DefaultDomain owners = region.getOwners();
        owners.addPlayer(creator.getUniqueId());
        region.setOwners(owners);

        applyIsolationFlags(region);

        regionManager.addRegion(region);

        RogueSmpCore.LOGGER.info("Successfully registered WorldGuard isolation matrix for region: {}", regionName);
    }

    /**
     * Tailors specific guard rules ensuring players outside the island team cannot interact with it.
     */
    private void applyIsolationFlags(ProtectedRegion region) {
        // Stop non-members from placing/breaking blocks
        region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);

        // Block interaction with chests, furnaces, or doors for non-members
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);

        // Prevent random players from wandering or flying directly inside the space
        region.setFlag(Flags.EXIT, StateFlag.State.DENY);
        region.setFlag(Flags.ENTRY, StateFlag.State.DENY);

        region.setFlag(Flags.BUILD.getRegionGroupFlag(), com.sk89q.worldguard.protection.flags.RegionGroup.MEMBERS);
        region.setFlag(Flags.CHEST_ACCESS.getRegionGroupFlag(), com.sk89q.worldguard.protection.flags.RegionGroup.MEMBERS);
        region.setFlag(Flags.INTERACT.getRegionGroupFlag(), com.sk89q.worldguard.protection.flags.RegionGroup.MEMBERS);
        region.setFlag(Flags.EXIT.getRegionGroupFlag(), com.sk89q.worldguard.protection.flags.RegionGroup.MEMBERS);
        region.setFlag(Flags.ENTRY.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
    }

    /**
     * Hooks into WorldGuard backend allocations to grant a player member rights to an island region.
     */
    public void addTeammateToIslandRegion(UUID teammateUuid, UUID islandId, World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) return;

        ProtectedRegion region = regionManager.getRegion(getRegionName(islandId));
        if (region != null) {
            DefaultDomain members = region.getMembers();
            members.addPlayer(teammateUuid);
            region.setMembers(members);
        }
    }

    public void removeTeammateFromIslandRegion(UUID teammateUuid, UUID islandId, World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) return;

        ProtectedRegion region = regionManager.getRegion(getRegionName(islandId));
        if (region != null) {
            DefaultDomain members = region.getMembers();
            members.removePlayer(teammateUuid);
            region.setMembers(members);
        }
    }

    public boolean isLocationInsideRegion(UUID islandId, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) return false;

        ProtectedRegion region = regionManager.getRegion(getRegionName(islandId));
        if (region == null) return false;
        return region.contains(BlockVector3.at(location.x(), location.y(), location.z()));
    }

    /**
     * Get the island owning the region at this location
     * @return The region, or null if no island region is found
     */
    public @Nullable ProtectedRegion getRegionAtLocation(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) return null;

        Set<ProtectedRegion> regionSet = regionManager.getApplicableRegions(BlockVector3.at(location.x(), location.y(), location.z())).getRegions();
        for (ProtectedRegion region : regionSet) {
            if (region.getId().startsWith(REGION_PREFIX)) return region;
        }
        return null;
    }

    public String getRegionName(UUID islandId) {
        return REGION_PREFIX + islandId.toString();
    }
}
