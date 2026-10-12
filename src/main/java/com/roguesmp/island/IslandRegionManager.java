package com.roguesmp.island;

import com.roguesmp.region.ServerRegionManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;

import java.util.UUID;

public class IslandRegionManager {
    private static final String REGION_ID = "island";
    private static final int WORLD_PADDING = 5;

    private final ServerRegionManager serverRegionManager = ServerRegionManager.getInstance();

    public void createIslandRegion(World world, UUID creatorId) {
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

        serverRegionManager.add(world, region);
        serverRegionManager.saveChanges(world);
    }

    public void addTeammate(World world, UUID teammateId) {
        serverRegionManager.editMembers(world, REGION_ID, members -> members.addPlayer(teammateId));
        serverRegionManager.saveChanges(world);
    }

    public void removeTeammate(World world, UUID teammateId) {
        serverRegionManager.editMembers(world, REGION_ID, members -> members.removePlayer(teammateId));
        serverRegionManager.saveChanges(world);
    }

    public void removeIslandRegion(World world) {
        serverRegionManager.remove(world, REGION_ID);
        serverRegionManager.saveChanges(world);
    }

    public void deleteStoredRegionData(String worldName) {
        serverRegionManager.deleteStoredData(worldName);
    }
}
