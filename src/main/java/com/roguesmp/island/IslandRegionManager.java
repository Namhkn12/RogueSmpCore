package com.roguesmp.island;

import com.roguesmp.region.ServerRegionManager;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.World;

import java.util.UUID;

public class IslandRegionManager {
    private final ServerRegionManager serverRegionManager = ServerRegionManager.getInstance();

    public void createIslandRegion(World world, UUID creatorId) {
        serverRegionManager.editGlobal(world, region -> {
            DefaultDomain members = region.getMembers();
            members.addPlayer(creatorId);
            region.setMembers(members);

            region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
            region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
            region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);

            region.setFlag(Flags.BUILD.getRegionGroupFlag(), RegionGroup.MEMBERS);
            region.setFlag(Flags.CHEST_ACCESS.getRegionGroupFlag(), RegionGroup.MEMBERS);
            region.setFlag(Flags.INTERACT.getRegionGroupFlag(), RegionGroup.MEMBERS);
        });
    }

    public void addTeammate(World world, UUID teammateId) {
        serverRegionManager.editMembers(world, ServerRegionManager.GLOBAL_ID, members -> members.addPlayer(teammateId));
        serverRegionManager.saveChanges(world);
    }

    public void removeTeammate(World world, UUID teammateId) {
        serverRegionManager.editMembers(world, ServerRegionManager.GLOBAL_ID, members -> members.removePlayer(teammateId));
        serverRegionManager.saveChanges(world);
    }

    public void removeIslandRegion(World world) {
        serverRegionManager.remove(world, ServerRegionManager.GLOBAL_ID);
        serverRegionManager.saveChanges(world);
    }

    public void deleteStoredRegionData(String worldName) {
        serverRegionManager.deleteStoredData(worldName);
    }
}
