package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.actor.ui.OpenDoorGui;
import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.CompletionScope;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.BaseRoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEventContext;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEventPhase;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventConfig;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventFactory;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.Region;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.helper.SerializableBounds;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import com.roguesmp.dungeon_v2.utils.UuidUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DungeonFlowController {

    private final IInstanceService instanceService;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final IRegionService regionService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;
    private final IRoomService roomService;
    private final DungeonPresenter dungeonPresenter;
    private final ISchematicService schematicService;
    private final IDungeonService dungeonService;
    private final TaskScheduler taskScheduler;
    private final IReviveService reviveService;

    public DungeonFlowController(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService,
                                 IRegionService regionService, ScoreBoardManager scoreBoardManager,
                                 DungeonManager dungeonManager, RoomManager roomManager, IRoomService roomService, DungeonPresenter dungeonPresenter, ISchematicService schematicService, IDungeonService dungeonService, TaskScheduler taskScheduler, IReviveService reviveService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.roomManager = roomManager;
        this.roomService = roomService;
        this.dungeonPresenter = dungeonPresenter;
        this.schematicService = schematicService;
        this.dungeonService = dungeonService;
        this.taskScheduler = taskScheduler;
        this.reviveService = reviveService;
    }

    public void handleStartDungeon(String did, Player player){
        /*Prepare data*/
        Party party = partyService.getPartyByPlayer(player);
        if(party == null){
            return;
        }
        Dungeon dungeon = dungeonManager.get(did);
        /*Start dungeon*/
        DungeonInstance instance = instanceService.createDungeonInstance(dungeon, party);
        if (instance == null || instance.getSession() == null || instance.getSession().getSessionId() == null) return;
        party.setInstanceId(instance.getSession().getSessionId());
        instanceManager.add(instance);
        /*Start instance*/
        instanceService.startDungeonInstance(instance);
        bindCurrentRoomRuntime(instance, party);
        RoomInstance startRoom = instance.getProgress().getCurrentRoom();
        triggerRoomStart(startRoom);
        if (startRoom != null && startRoom.isCompleted()) {
            triggerRoomEnd(startRoom);
        }
        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        if (region == null) return;
        /*Teleport player to spawn room (region point)*/
        Teleporter.teleportAllByIdString(party.getMembers(), region.getRegionPoint());
        /*Presentation*/
        partyService.getOnlineMembers(party).forEach(p -> {
            /*Build scoreboard*/
            scoreBoardManager.createBoard(p, instance, dungeon);
            /*Build Effect and Sound*/
            dungeonPresenter.onEnterDungeon(p, dungeon.getName());
        });
    }

    public void handleOpenNextDoor(Block door, Player player){
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        DungeonProgress progress = instance.getProgress();

        /*Boss defeated → open treasure portal*/
        if(progress.getStatus() == DungeonProgress.Status.BOSS_DEFEATED){
            handleOpenTreasurePortal(door);
            return;
        }

        /*Check current room completed*/
        if(!progress.getCurrentRoom().isCompleted()) return;

        if(progress.getNextRooms() == null || progress.getNextRooms().isEmpty()){
            String currentRoomId = progress.getCurrentRoom() != null ? progress.getCurrentRoom().getRoomId() : null;
            List<String> candidatePool = progress.getRoomPool().stream()
                    .filter(roomId -> currentRoomId == null || !currentRoomId.equals(roomId))
                    .toList();

            List<String> nextRoomIds = dungeonService.rollNextRoomFromPool(
                    candidatePool,
                    progress.getMinimumRooms(),
                    progress.getClearedRooms()
            );
            progress.setNextRooms(nextRoomIds);
            instanceManager.add(instance);
        }

        List<Room> nextRooms = progress.getNextRooms()
                .stream()
                .map(roomManager::get)
                .toList();

        new OpenDoorGui(door, nextRooms, this::handleSelectNextRoom).showInventory(player);
    }

    public void handleSelectNextRoom(Player player, Block door, Room room){
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        DungeonProgress progress = instance.getProgress();
        Location doorLoc = door.getLocation();
        Location pasteLoc = doorLoc.clone().add(0, -2, 0);

        /*Build selected room*/
        BoundingBox area = schematicService.paste(room.getSchemetaId(), pasteLoc);
        RoomInstance roomInstance = new RoomInstance(room.getId(), SerializableBounds.from(area));
        setupRoomRuntime(room, roomInstance, party);
        roomInstance.setDoor(SerializableLocation.from(doorLoc));

        /*Mark current room cleared, set new room*/
        progress.markCurrentRoomCleared();
        progress.setCurrentRoom(roomInstance);
        progress.setNextRooms(null);
        triggerRoomStart(roomInstance);
        if (roomInstance.isCompleted()) {
            triggerRoomEnd(roomInstance);
        }

        /*Open the door*/
        roomService.openRoomDoor(doorLoc);

        /*Presenter*/
        party.getMembers().forEach(playerId -> {
            Player member = UuidUtil.getPlayerById(playerId);
            if (member == null) return;
            dungeonPresenter.onOpenDoor(member);
        });

        /*Close the door after 5 seconds and teleport player if they outside the bounding box*/
        if(!roomInstance.isCompleted() && room.getType().isCombat()){
            taskScheduler.runLater(100L, () -> {
                List<Player> members = partyService.getOnlineMembers(party);
                BoundingBox activeZone = roomInstance.getBounds().toBukkit();

                for (Player member : members) {
                    if (!member.isOnline()) continue;

                    if (!activeZone.contains(member.getLocation().toVector())) {
                        Location insideLoc = doorLoc.clone().add(0, 0, -2);
                        member.teleport(insideLoc);
                    }
                }

                roomService.closeRoomDoor(doorLoc);
            });
        }
    }

    public void handleOpenTreasurePortal(Block door){
        Location doorLoc = door.getLocation();
        World world = doorLoc.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(doorLoc.clone().add(dx, dy, 0)).setType(Material.END_GATEWAY);
            }
        }
    }

    public void handleGetIntoTreasurePortal(Player player){
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        /*Paste treasure room for each player*/
        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        if (region == null) return;
        Location location = region.getRegionPoint();
        String treasureRoomId = instance.getProgress().getTreasureRoomId();
        int space = 32;
        int count = instance.getProgress().getFinalRewardBuildCount() + 1;
        instance.getProgress().setFinalRewardBuildCount(count);
        Room room = roomManager.get(treasureRoomId);
        Location treasureSpawn = location.add(0, space*count, 0);
        schematicService.paste(room.getSchemetaId(), treasureSpawn);
        Teleporter.teleport(player, location.add(0, 1, 0));
    }

    public void handleLeaveDungeon(Player player){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null){
            DungeonEcho.warn(player, "Is you still in party?");
            return;
        }
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if(instance == null){
            DungeonEcho.warn(player, "Is your party still in dungeon?");
            return;
        }
        /*TODO: implement GUI for confirming leave dungeon*/
        /*Remove tracking player from dungeon instance*/
        Map<String, PlayerStatus> players = instance.getDungeonPlayers().getPlayers();
        players.remove(player.getUniqueId().toString());
        if(instance.getProgress().getStatus() == DungeonProgress.Status.IN_PROGRESS){
            party.removeMember(player.getUniqueId().toString());
            instance.getDungeonPlayers().setPlayerStatus(player.getUniqueId().toString(), PlayerStatus.Status.OUT);
            DungeonEcho.info(player, "Bạn đã rời khỏi dungeon khi đang chơi," +
                    "đồng thời cũng sẽ rời khỏi party!");
        }
        Teleporter.teleport(player, SerializableLocation.from(new Location(Bukkit.getWorld("building"), 0, 0, 0)));
        scoreBoardManager.removeBoard(player);
        /*If all players in dungeon leave, remove dungeon instance*/
        if(players.isEmpty()){
            instanceService.removeDungeonInstance(instance);
        }
    }

    public void handleEndUpDungeon(DungeonInstance instance){
        DungeonPlayer dungeonPlayer = instance.getDungeonPlayers();
        dungeonPlayer.getPlayers().forEach((uuid, playerStatus) -> {
            Player player = UuidUtil.getPlayerById(uuid);
            if(player != null && playerStatus.getStatus() == PlayerStatus.Status.PLAYING){
                /*Notice player that dungeon will be destroyed*/
                if(instance.getProgress().getStatus() == DungeonProgress.Status.FAILED){
                    DungeonEcho.warn(player, "The dungeon has collapsed! You barely escaped...");
                } else {
                    DungeonEcho.success(player, "The dungeon has been conquered! Glory to your party!");
                }
                /*Remove score board*/
                scoreBoardManager.removeBoard(player);
                /*Teleport player*/
                Teleporter.teleport(player, SerializableLocation.from(new Location(Bukkit.getWorld("building"), 0, 0, 0)));
            }
        });
        instanceService.removeDungeonInstance(instance);
        instanceManager.remove(instance.getSession().getSessionId());
    }

    public void handleDungeonTimerTick() {
        for (DungeonInstance instance : instanceManager.getAll()) {
            if (instance == null || instance.getTimer() == null || instance.getProgress() == null) continue;

            DungeonProgress.Status status = instance.getProgress().getStatus();
            if (status == DungeonProgress.Status.COMPLETED || status == DungeonProgress.Status.FAILED) continue;

            Party party = partyService.getPartyById(instance.getSession().getPartyId());
            if (party != null) {
                bindCurrentRoomRuntime(instance, party);
            }
            triggerRoomTick(instance.getProgress().getCurrentRoom());

            if (!instance.getTimer().isExpired()) continue;
            handleDungeonTimeExpired(instance);
        }
    }

    /**
     * Timeout hook. Default behavior marks run as FAILED and forwards to end handler.
     * Bạn có thể đổi luồng cleanup/reward ở handleEndUpDungeon(...).
     */
    public void handleDungeonTimeExpired(DungeonInstance instance) {
        if (instance == null || instance.getProgress() == null) return;
        DungeonProgress.Status status = instance.getProgress().getStatus();
        if (status == DungeonProgress.Status.COMPLETED || status == DungeonProgress.Status.FAILED) return;

        instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
        handleEndUpDungeon(instance);
    }

    public void handleDungeonFinish(DungeonInstance instance){

    }

    private void bindCurrentRoomRuntime(DungeonInstance instance, Party party) {
        if (instance == null || instance.getProgress() == null || party == null) return;
        RoomInstance roomInstance = instance.getProgress().getCurrentRoom();
        if (roomInstance == null) return;

        Room roomTemplate = roomManager.get(roomInstance.getRoomId());
        if (roomTemplate == null) return;

        setupRoomRuntime(roomTemplate, roomInstance, party);
    }

    private void setupRoomRuntime(Room roomTemplate, RoomInstance roomInstance, Party party) {
        prepareObjectives(roomTemplate, roomInstance, party);
        prepareRoomEvents(roomTemplate, roomInstance, party);

        boolean noObjectives = roomInstance.getActiveObjectives() == null || roomInstance.getActiveObjectives().isEmpty();
        if (noObjectives) {
            roomInstance.setCompleted(true);
        }
    }

    private void prepareObjectives(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<IObjective> objectives = roomInstance.getActiveObjectives();
        if (objectives == null || objectives.isEmpty()) {
            objectives = new ArrayList<>();
            for (ObjectiveConfig config : roomTemplate.getObjectives()) {
                objectives.add(ObjectiveFactory.create(config));
            }
            roomInstance.setActiveObjectives(objectives);
        }

        for (IObjective objective : objectives) {
            if (objective instanceof BaseObjective baseObjective) {
                baseObjective.setCallback(o -> {
                    DungeonInstance instance = instanceManager.get(party.getInstanceId());
                    if (instance == null) return;
                    /*Plus score when finish*/
                    int gained = baseObjective.getScore();
                    instance.getProgress().setScore(
                            instance.getProgress().getScore() + gained
                    );
                    /*Check room complete*/
                    if (baseObjective.getCompletionScope() == CompletionScope.DUNGEON) {
                        checkDungeonCompletion(instance, party);
                    } else {
                        checkRoomCompletion(instance, roomInstance, party);
                    }
                });
            }
        }
    }

    private void prepareRoomEvents(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<RoomEvent> events = roomInstance.getActiveRoomEvents();
        if (events == null || events.isEmpty()) {
            events = new ArrayList<>();
            for (RoomEventConfig config : roomTemplate.getRoomEvents()) {
                events.add(RoomEventFactory.create(config));
            }
            roomInstance.setActiveRoomEvents(events);
        }

        for (RoomEvent event : events) {
            if (event instanceof BaseRoomEvent baseEvent) {
                baseEvent.setContext(new RoomEventContext(roomInstance, party, partyService));
                baseEvent.setCallback((roomEvent, phase) -> onRoomEventCallback(roomEvent, phase, roomInstance, party));
            }
        }
    }

    private void triggerRoomStart(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomStart);
    }

    private void triggerRoomTick(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomPlay);
    }

    private void triggerRoomEnd(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomEnd);
    }

    private void onRoomEventCallback(RoomEvent event, RoomEventPhase phase, RoomInstance roomInstance, Party party) {
        // Hook for room-event side effects outside the event class itself.
    }

    private void checkRoomCompletion(DungeonInstance instance, RoomInstance room, Party party) {
        if (room == null || room.getActiveObjectives() == null) return;

        boolean allDone = room.getActiveObjectives().stream()
                .filter(o -> o instanceof BaseObjective)
                .map(o -> (BaseObjective) o)
                .allMatch(BaseObjective::isCompleted);

        if (allDone) {
            room.setCompleted(true);
            triggerRoomEnd(room);
            DungeonEcho.success(partyService.getOnlineMembers(party), "Room clear!");
            revivePlayer(instance);
            /*Open the back door*/
            if (room.getDoor() != null) {
                roomService.openRoomDoor(room.getDoor().toBukkit());
            }
            /*Presenter*/
            partyService.getOnlineMembers(party).forEach(dungeonPresenter::onCompleteRoom);

        }
    }

    private void checkDungeonCompletion(DungeonInstance instance, Party party) {
        instance.getProgress().setStatus(DungeonProgress.Status.BOSS_DEFEATED);
        /*Set the remaining time to 5 minute*/
        long rewardWindowMs = 5 * 60 * 1000L;
        long newEndTime = System.currentTimeMillis() + rewardWindowMs;
        instance.getTimer().setEndTime(newEndTime);
        /*Message to all players that the boss are defeated*/
        DungeonEcho.success(partyService.getOnlineMembers(party), "Boss defeated!!!");
        revivePlayer(instance);
        /*Presenter*/
        partyService.getOnlineMembers(party).forEach(dungeonPresenter::onCompleteDungeon);
    }

    private void revivePlayer(DungeonInstance instance){
        instance.getDungeonPlayers().getPlayers().forEach((uuid, playerStatus) -> {
            if(playerStatus.getStatus() == PlayerStatus.Status.DEAD){
                playerStatus.setStatus(PlayerStatus.Status.PLAYING);

                /*Clean up revive entry*/
                reviveService.forceRemoveDeadEntry(UuidUtil.parseOrNull(uuid));
                Player player = UuidUtil.getPlayerById(uuid);
                if(player != null) {
                    player.teleport(playerStatus.getCheckPoint().toBukkit());
                    player.setGameMode(GameMode.SURVIVAL);
                    dungeonPresenter.onPlayerRevive(player, playerStatus.getCheckPoint().toBukkit());
                    DungeonEcho.success(player, "Fate grants you another chance!");
                }
            }
        });
    }
}
