package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.actor.scoreboard.MemberStatus;
import com.roguesmp.dungeon.constant.RoomType;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.dto.NextRoom;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.manager.ScoreBoardManager;
import com.roguesmp.dungeon.objective_.IObjective;
import com.roguesmp.dungeon.objective_.ObjectiveFactory;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.Log4Craft;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class DungeonFlowService implements IDungeonFlowService {

    private final IPartyService partyService;
    private final IInstanceService instanceService;
    private final IDungeonService dungeonService;
    private final ISchemetaService schemetaService;
    private final IRegionService regionService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonPresenter dungeonPresenter;


    public DungeonFlowService(IPartyService partyService,
                              IInstanceService instanceService, IDungeonService dungeonService, ISchemetaService schemetaService, IRegionService regionService, ScoreBoardManager scoreBoardManager, DungeonPresenter dungeonPresenter) {
        this.partyService = partyService;
        this.instanceService = instanceService;
        this.dungeonService = dungeonService;
        this.schemetaService = schemetaService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonPresenter = dungeonPresenter;
    }

    @Override
    public DungeonInstance onGenerateDungeon(@NotNull String dungeonId, @NotNull Player partyOwner) {
        if (!partyService.isOwner(partyOwner)){
            DungeonEcho.error(partyOwner, "You are not party owner!");
            return null;
        }
        Party party = partyService.getPartyByPlayer(partyOwner).orElse(null);
        if(party == null){
            DungeonEcho.error(partyOwner, "Party does not exist!");
            return null;
        }
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(dungeonId);

        if(instanceService.hasActiveInstance(party.getPartyId())){
            DungeonEcho.error(partyOwner, "This party is in an existing dungeon");
        }

        if (dungeon.isEmpty()) {
            DungeonEcho.error(partyOwner, "Dungeon not found");
            return null;
        }

        Region region = regionService.acquireRegion().orElse(null);
        if (region == null) {
            DungeonEcho.error(partyOwner, "Cannot locate a dungeon region");
            return null;
        }

        RegionInstance regionInstance = new RegionInstance(
                region.getId(),
                region.getWorldName(),
                region.getRegionPoint()
        );

        return instanceService.createDungeonInstance(
                dungeon.get(),
                party.getPartyId(),
                regionInstance
        );
    }

    @Override
    public boolean onStartDungeon(DungeonInstance instance) {
        Party party = partyService.getPartyById(instance.getParty()).orElse(null);
        if(party == null){
            Log4Craft.error("Party is null. Please check!");
            return false;
        }

        UUID partyId = party.getPartyId();
        Player partyOwner = Bukkit.getPlayer(party.getOwner());
        if(partyOwner == null){
            return false;
        }

        RegionInstance region = instance.getRegion();
        Map<UUID, NodeInstance> nodes = instance.getNodes();

        /*Warning*/
        /*Search start node in instance (need to fallback if start node was not found)*/
        NodeInstance startNode = nodes.values().stream()
                .filter(n -> RoomType.START.getJsonKey().equals(n.getNodeKey()))
                .findFirst()
                .orElse(null);
        if (startNode == null){
            //fallback a default start room node
            return false;
        }

        /*Paste schematic*/
        try {
            schemetaService.pasteSchematic(startNode.getSchemetas(), region.getLocation());
        } catch (BaseException e) {
            GlobalException.handle(e);
            //fallback - cancel dungeon
            return false;
        } catch (Exception e) {
            GlobalException.handleUnexpected("start dungeon: paste schematic", e);
            //fallback - cancel dungeon
            return false;
        }

        /*Set active room*/
        RoomInstance startRoom = new RoomInstance(startNode, null, null, true, null);
        instance.setActiveRoom(startRoom);

        /*Remove start node then roll next rooms*/
        nodes.remove(startNode.getId());
        instanceService.rollNextRooms(instance);
        Dungeon template = dungeonService.getDungeonById(instance.getDungeon()).orElse(null);

        /*Scoreboard*/
        scoreBoardManager.bind(instance);
        List<DungeonScoreBoard.PartyMember> members = buildPartyMembers(party);

        int slot = 0;
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.teleport(region.getLocation());
                dungeonPresenter.onEnterDungeon(member, template.getDgName());
                /*Build scoreboard*/
                scoreBoardManager.createBoard(member, slot, members, instance, template);
            }
            slot++;
        }

        /*Save the instance to file for the first time*/
        instanceService.saveInstance(partyId);
        return true;
    }

    @Override
    public List<NextRoom> onOpenDoorGui(DungeonInstance instance) {
        Map<UUID, Integer> nextRoomInstance = instance.getNextRooms();
        Map<UUID, NodeInstance> nodePools = instance.getNodes();

        if (nextRoomInstance == null || nextRoomInstance.isEmpty())
            return new ArrayList<>();

        List<NextRoom> nextRooms = new ArrayList<>();
        nextRoomInstance.forEach((ri, i) -> {
            NodeInstance nodeInstance = nodePools.get(ri);
            if (nodeInstance == null) return; // fallback: bỏ qua node lỗi, cần sử lý sau
            nextRooms.add(new NextRoom(nodeInstance, i));
        });

        return nextRooms;
    }

    @Override
    public RoomInstance onOpenNextRoom(@NonNull DungeonInstance dungeonInstance, @NonNull NodeInstance selectNode) {
        String schemetaId = selectNode.getSchemetas();
        Schemeta schemeta = schemetaService.getSchemeta(schemetaId);

        List<IObjective> objectives = new ArrayList<>();
        List<ObjectiveData> objectiveData = schemeta.getObjectives();
        if (objectiveData != null && !objectiveData.isEmpty()) {
            objectiveData.forEach(oj -> objectives.add(ObjectiveFactory.create(oj)));
        }

        RoomInstance roomInstance = new RoomInstance(selectNode, null, objectives, objectives.isEmpty(), null);

        Optional<Party> party = partyService.getPartyById(dungeonInstance.getParty());
        if(party.isEmpty()) return null;

        objectives.forEach(obj -> {
            obj.callBack(completedObj -> {
                dungeonInstance.setScore(dungeonInstance.getScore() + completedObj.getScore());
                scoreBoardManager.onScoreChanged(dungeonInstance);

                boolean allCompleted = roomInstance.getObjective().stream()
                        .allMatch(IObjective::isCompleted);

                if (allCompleted) {
                    roomInstance.setCompleted(true);
                    party.get().getMembers().forEach(playerId -> {
                        Player member = Bukkit.getPlayer(playerId);
                        dungeonPresenter.onCompleteRoom(member);
                    });

                    onRoomCompleted(dungeonInstance, roomInstance);
                }
            });
            obj.start();

            party.ifPresent(p -> p.getMembers().forEach(memberId -> {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) member.sendMessage(obj.getMessage());
            }));
        });

        dungeonInstance.getCompletedRooms().add(dungeonInstance.getActiveRoom());
        dungeonInstance.setActiveRoom(roomInstance);
        dungeonInstance.getNodes().remove(selectNode.getId());
        instanceService.rollNextRooms(dungeonInstance);
        party.get().getMembers().forEach(playerId -> {
            Player member = Bukkit.getPlayer(playerId);
            dungeonPresenter.onOpenDoor(member);
        });
        return dungeonInstance.getActiveRoom();
    }

    @Override
    public void onRoomCompleted(DungeonInstance dungeon, RoomInstance room) {

        NodeInstance node = room.getNode();

        if (Objects.equals(node.getNodeKey(), "end")) {
            completeDungeon(dungeon);
            return;
        }
    }

    @Override
    public void onFinishDungeon() {

    }

    private void completeDungeon(DungeonInstance dungeon) {
        if (dungeon.isCompleted()) return;

        dungeon.setCompleted(true);

        partyService.getPartyById(dungeon.getParty()).ifPresent(p ->
                p.getMembers().forEach(id -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        dungeonPresenter.onCompleteDungeon(player);
                    }
                })
        );
        //TO DO
        //chỉnh thời gian lên 10p cuối để player nhận thưởng ở cổng cuối cùng
    }

    private void finishDungeonProcess(DungeonInstance dungeon){
        partyService.getPartyById(dungeon.getParty()).ifPresent(p ->
                p.getMembers().forEach(id -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        //tp ve the gioi cu
                    }
                })
        );

        instanceService.endDungeonInstance(dungeon.getParty());
    }

    private List<DungeonScoreBoard.PartyMember> buildPartyMembers(Party party) {
        List<DungeonScoreBoard.PartyMember> members = new ArrayList<>();
        int slot = 0;

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            MemberStatus status = (member != null && member.isOnline())
                    ? MemberStatus.ALIVE
                    : MemberStatus.OFFLINE;

            String displayName = member != null
                    ? member.getName()
                    : memberId.toString().substring(0, 8); // fallback nếu offline

            members.add(new DungeonScoreBoard.PartyMember(displayName, status));
            slot++;
        }

        return members;
    }
}
