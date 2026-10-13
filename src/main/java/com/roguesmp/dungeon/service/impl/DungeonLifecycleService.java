package com.roguesmp.dungeon.service.impl;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.Region;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.helper.SerializableLocation;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.manager.ScoreBoardManager;
import com.roguesmp.dungeon_spawner.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.IInstanceService;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.service.IReviveService;
import com.roguesmp.dungeon.utils.Teleporter;
import com.roguesmp.dungeon.utils.UuidUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;

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
    private final IReviveService reviveService;

    public DungeonLifecycleService(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService, IRegionService regionService, ScoreBoardManager scoreBoardManager, DungeonManager dungeonManager, SpawnerInstanceManager spawnerInstanceManager, DungeonPresenter presenter, RoomRuntimeService roomRuntimeService, IReviveService reviveService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.presenter = presenter;
        this.roomRuntimeService = roomRuntimeService;
        this.reviveService = reviveService;
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
        if (party == null) { player.sendMessage(Component.text("Is you still in party?", NamedTextColor.YELLOW)); return; }
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) { player.sendMessage(Component.text("Is your party still in dungeon?", NamedTextColor.YELLOW)); return; }

        Map<String, PlayerStatus> players = instance.getDungeonPlayers().getPlayers();
        players.remove(player.getUniqueId().toString());

        if (instance.getProgress().getStatus() == DungeonProgress.Status.IN_PROGRESS) {
            partyService.forceKick(player);
            instance.getDungeonPlayers().setPlayerStatus(
                    player.getUniqueId().toString(), PlayerStatus.Status.OUT);
            player.sendMessage(Component.text("Bạn đã rời khỏi dungeon khi đang chơi, đồng thời cũng sẽ rời khỏi party!", NamedTextColor.GRAY));
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
                player.sendMessage(Component.text("The dungeon has collapsed! You barely escaped...", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("The dungeon has been conquered! Glory to your party!", NamedTextColor.GREEN));
            }
            scoreBoardManager.removeBoard(player);
            Teleporter.teleport(player,
                    SerializableLocation.from(new Location(Bukkit.getWorld("building"), 0, 0, 0)));
        });

        instanceService.removeDungeonInstance(instance);
        instanceManager.remove(instance.getSession().getSessionId());
        Party party = partyService.getPartyById(instance.getSession().getPartyId());
        party.setInstanceId("");
        party.getMembers().forEach(s -> {
            Player p = UuidUtil.getPlayerById(s);
            if(p != null) reviveService.forceRemoveDeadEntry(p.getUniqueId());
        });
    }

    public void onDungeonTimeExpired(DungeonInstance instance) {
        if (instance == null || instance.getProgress() == null) return;
        DungeonProgress.Status status = instance.getProgress().getStatus();
        if (status == DungeonProgress.Status.COMPLETED) return;
        instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
        onEndUpDungeon(instance);
    }

    public void onDungeonFinish(DungeonInstance instance) {
        roomRuntimeService.clearEntitiesInDungeon(instance);
        long newEndTime = System.currentTimeMillis() + 10 * 1000L;
        instance.getTimer().setEndTime(newEndTime);
        instance.getDungeonPlayers().getPlayers().forEach((s, playerStatus) -> {
            Player p = UuidUtil.getPlayerById(s);
            if(p == null) return;
            p.sendMessage(Component.text("Your entire party has fallen — the dungeon claims victory. " +
                    "It will collapse in 10 seconds!", NamedTextColor.RED));
        });
    }


}