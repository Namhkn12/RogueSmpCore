package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.Region;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IInstanceService;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IRegionService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import com.roguesmp.dungeon_v2.utils.UuidUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

public class DungeonLifecycleService {

    private final IInstanceService instanceService;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final IRegionService regionService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonManager dungeonManager;
    private final DungeonPresenter presenter;
    private final RoomRuntimeService roomRuntimeService;

    public DungeonLifecycleService(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService, IRegionService regionService, ScoreBoardManager scoreBoardManager, DungeonManager dungeonManager, DungeonPresenter presenter, RoomRuntimeService roomRuntimeService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.presenter = presenter;
        this.roomRuntimeService = roomRuntimeService;
    }


    public void onStartDungeon(String did, Player player) {
        Party party = partyService.getPartyByPlayer(player);
        if (party == null) return;
        Dungeon dungeon = dungeonManager.get(did);

        DungeonInstance instance = instanceService.createDungeonInstance(dungeon, party);
        if (instance == null || instance.getSession() == null
                || instance.getSession().getSessionId() == null) return;

        party.setInstanceId(instance.getSession().getSessionId());
        instanceManager.add(instance);
        instanceService.startDungeonInstance(instance);

        roomRuntimeService.bindCurrentRoomRuntime(instance, party);
        RoomInstance startRoom = instance.getProgress().getCurrentRoom();
        roomRuntimeService.triggerRoomStart(startRoom);
        if (startRoom != null && startRoom.isCompleted()) {
            roomRuntimeService.triggerRoomEnd(startRoom);
        }

        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        if (region == null) return;

        Teleporter.teleportAllByIdString(party.getMembers(), region.getRegionPoint());
        partyService.getOnlineMembers(party).forEach(p -> {
            scoreBoardManager.createBoard(p, instance, dungeon);
            presenter.onEnterDungeon(p, dungeon.getName());
        });
    }

    public void onLeaveDungeon(Player player) {
        Party party = partyService.getPartyByPlayer(player);
        if (party == null) { DungeonEcho.warn(player, "Is you still in party?"); return; }
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) { DungeonEcho.warn(player, "Is your party still in dungeon?"); return; }

        Map<String, PlayerStatus> players = instance.getDungeonPlayers().getPlayers();
        players.remove(player.getUniqueId().toString());

        if (instance.getProgress().getStatus() == DungeonProgress.Status.IN_PROGRESS) {
            partyService.forceKick(player);
            instance.getDungeonPlayers().setPlayerStatus(
                    player.getUniqueId().toString(), PlayerStatus.Status.OUT);
            DungeonEcho.info(player, "Bạn đã rời khỏi dungeon khi đang chơi, đồng thời cũng sẽ rời khỏi party!");
        }

        Teleporter.teleport(player,
                SerializableLocation.from(new Location(Bukkit.getWorld("building"), 0, 0, 0)));
        scoreBoardManager.removeBoard(player);

        if (players.isEmpty()) {
            instanceService.removeDungeonInstance(instance);
            party.setInstanceId("");
        }
    }

    public void onEndUpDungeon(DungeonInstance instance) {
        instance.getDungeonPlayers().getPlayers().forEach((uuid, playerStatus) -> {
            Player player = UuidUtil.getPlayerById(uuid);
            if (player == null) return;
            if (instance.getProgress().getStatus() == DungeonProgress.Status.FAILED) {
                DungeonEcho.warn(player, "The dungeon has collapsed! You barely escaped...");
            } else {
                DungeonEcho.success(player, "The dungeon has been conquered! Glory to your party!");
            }
            scoreBoardManager.removeBoard(player);
            Teleporter.teleport(player,
                    SerializableLocation.from(new Location(Bukkit.getWorld("building"), 0, 0, 0)));
        });

        instanceService.removeDungeonInstance(instance);
        instanceManager.remove(instance.getSession().getSessionId());
        partyService.getPartyById(instance.getSession().getPartyId()).setInstanceId("");
    }

    public void onDungeonTimeExpired(DungeonInstance instance) {
        if (instance == null || instance.getProgress() == null) return;
        DungeonProgress.Status status = instance.getProgress().getStatus();
        if (status == DungeonProgress.Status.COMPLETED
                || status == DungeonProgress.Status.FAILED) return;
        instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
        onEndUpDungeon(instance);
    }

    public void onDungeonFinish(DungeonInstance instance) {
        // TODO
    }
}