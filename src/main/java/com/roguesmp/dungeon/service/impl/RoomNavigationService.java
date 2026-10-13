package com.roguesmp.dungeon.service.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.roguesmp.dungeon.actor.ui.OpenDoorGui;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.helper.SerializableBounds;
import com.roguesmp.dungeon.helper.SerializableLocation;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.IDungeonService;
import com.roguesmp.dungeon_party.PartyManager;
import com.roguesmp.dungeon.service.IRoomService;
import com.roguesmp.dungeon.service.ISchematicService;
import com.roguesmp.dungeon.utils.UuidUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

public class RoomNavigationService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;
    private final RoomManager roomManager;
    private final ISchematicService schematicService;
    private final IDungeonService dungeonService;
    private final IRoomService roomService;
    private final RoomRuntimeService roomRuntimeService;
    private final DungeonPresenter presenter;
    private final Plugin plugin;

    public RoomNavigationService(PartyManager partyManager, InstanceManager instanceManager, RoomManager roomManager, ISchematicService schematicService, IDungeonService dungeonService, IRoomService roomService, RoomRuntimeService roomRuntimeService, DungeonPresenter presenter, Plugin plugin){

        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
        this.roomManager = roomManager;
        this.schematicService = schematicService;
        this.dungeonService = dungeonService;
        this.roomService = roomService;
        this.roomRuntimeService = roomRuntimeService;
        this.presenter = presenter;
        this.plugin = plugin;
    }

    public void handleOpenNextDoor(Block door, Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        DungeonProgress progress = instance.getProgress();

        if (progress.getStatus() == DungeonProgress.Status.BOSS_DEFEATED) {
            handleOpenTreasurePortal(door);
            return;
        }

        if (!progress.getCurrentRoom().isCompleted()) return;

        if (progress.getNextRooms() == null || progress.getNextRooms().isEmpty()) {
            String currentRoomId = progress.getCurrentRoom() != null
                    ? progress.getCurrentRoom().getRoomId() : null;

            List<String> candidatePool = progress.getRoomPool().stream()
                    .filter(id -> currentRoomId == null || !currentRoomId.equals(id))
                    .toList();

            List<String> nextRoomIds = dungeonService.rollNextRoomFromPool(
                    candidatePool, progress.getMinimumRooms(), progress.getClearedRooms());

            progress.setNextRooms(nextRoomIds);
            instanceManager.add(instance);
        }

        List<Room> nextRooms = progress.getNextRooms().stream()
                .map(roomManager::get).toList();

        new OpenDoorGui(door, nextRooms, this::handleSelectNextRoom).showInventory(player);
    }

    public void handleSelectNextRoom(Player player, Block door, Room room) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        DungeonProgress progress = instance.getProgress();

        Location doorLoc = door.getLocation();
        Location pasteLoc = doorLoc.clone().add(0, -2, 0);

        BoundingBox area = schematicService.paste(room.getSchemetaId(), pasteLoc);
        RoomInstance roomInstance = new RoomInstance(room.getId(), SerializableBounds.from(area));
        roomRuntimeService.setupRoomRuntime(room, roomInstance, party);
        roomInstance.setDoor(SerializableLocation.from(doorLoc));

        progress.markCurrentRoomCleared();
        progress.setCurrentRoom(roomInstance);
        progress.setNextRooms(null);

        roomRuntimeService.triggerRoomStart(roomInstance);
        if (roomInstance.isCompleted()) roomRuntimeService.triggerRoomEnd(roomInstance);

        roomService.openRoomDoor(doorLoc);

        party.getMembers().forEach(playerId -> {
            Player member = UuidUtil.getPlayerById(playerId);
            if (member != null) presenter.onOpenDoor(member);
        });

        if (!roomInstance.isCompleted() && room.getType().isCombat()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                BoundingBox activeZone = roomInstance.getBounds().toBukkit();
                for (Player member : partyManager.getOnlineMembers(party)) {
                    if (!member.isOnline()) continue;
                    if (!activeZone.contains(member.getLocation().toVector())) {
                        member.teleport(doorLoc.clone().add(0, 0, -2));
                    }
                }
                roomService.closeRoomDoor(doorLoc);
            }, 100L);
        }
    }

    private void handleOpenTreasurePortal(Block door) {
        // delegate sang TreasureService nếu muốn
        Location loc = door.getLocation();
        World world = loc.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(loc.clone().add(dx, dy, 0)).setType(Material.END_GATEWAY);
            }
        }
    }
}
