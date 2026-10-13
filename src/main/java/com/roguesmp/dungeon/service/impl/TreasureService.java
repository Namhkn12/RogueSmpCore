package com.roguesmp.dungeon.service.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.Region;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon_party.PartyManager;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.service.ISchematicService;
import com.roguesmp.dungeon.utils.Teleporter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TreasureService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;
    private final IRegionService regionService;
    private final RoomManager roomManager;
    private final ISchematicService schematicService;
    private final Plugin plugin;

    public TreasureService(PartyManager partyManager, InstanceManager instanceManager, IRegionService regionService, RoomManager roomManager, ISchematicService schematicService, Plugin plugin) {
        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
        this.regionService = regionService;
        this.roomManager = roomManager;
        this.schematicService = schematicService;
        this.plugin = plugin;
    }


    public void handleOpenTreasurePortal(Block door) {
        Location loc = door.getLocation();
        World world = loc.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(loc.clone().add(dx, dy, 0)).setType(Material.END_GATEWAY);
            }
        }
    }

    public void handleGetIntoTreasurePortal(Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        if (region == null) return;
        Room room = roomManager.get(instance.getProgress().getTreasureRoomId());
        if (room == null) return;

        int space = 32;
        int count = instance.getProgress().getFinalRewardBuildCount() + 1;
        instance.getProgress().setFinalRewardBuildCount(count);

        Location treasureSpawn = region.getRegionPoint().clone().add(0, space * count, 0);
        Location teleportTarget = treasureSpawn.clone().add(0, 1, 0);

        schematicService.paste(room.getSchemetaId(), treasureSpawn);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Teleporter.teleport(player, teleportTarget), 20L);
    }
}
