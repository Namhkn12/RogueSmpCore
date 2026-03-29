package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.actor.scoreboard.MemberStatus;
import com.roguesmp.dungeon.dto.ActionResult;
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
import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import com.roguesmp.dungeon.objective_.ObjectiveFactory;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class DungeonController {

    private final IPartyService partyService;
    private final ISchemetaService schemetaService;
    private final IInstanceService instanceService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonPresenter dungeonPresenter;
    private final IDungeonFlowService dungeonFlowService;

    public DungeonController(IPartyService partyService,
                             ISchemetaService schemetaService, IInstanceService instanceService,
                             ScoreBoardManager scoreBoardManager, DungeonPresenter dungeonPresenter, IDungeonFlowService dungeonFlowService) {
        this.partyService = partyService;
        this.schemetaService = schemetaService;
        this.instanceService = instanceService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonPresenter = dungeonPresenter;
        this.dungeonFlowService = dungeonFlowService;
    }

    public ActionResult<Void> handleRequestDungeon(String dungeonId, Player player){
        DungeonInstance instance = dungeonFlowService.onGenerateDungeon(dungeonId, player);
        if(instance == null){
            //fallback - end dungeon
            return ActionResult.failed("Dungeon instance is null! Generate failed");
        }
        boolean result = dungeonFlowService.onStartDungeon(instance);
        if(!result) return ActionResult.failed("Cannot generate and start the dungeon");
        return ActionResult.ok("Dungeon with id " + instance.getUuid() + " request by " + player.getName() + " started");
    }

    // Trong startDungeon, sau khi teleport party
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

    public ActionResult<List<NextRoom>> getNextRooms(DungeonInstance instance) {
        Map<UUID, Integer> nextRoomInstance = instance.getNextRooms();
        Map<UUID, NodeInstance> nodePools = instance.getNodes();

        if (nextRoomInstance == null || nextRoomInstance.isEmpty())
            return ActionResult.ok("Đã đi hết phòng dungeon", new ArrayList<>());

        List<NextRoom> nextRooms = new ArrayList<>();
        nextRoomInstance.forEach((ri, i) -> {
            NodeInstance nodeInstance = nodePools.get(ri);
            if (nodeInstance == null) return; // fallback: bỏ qua node lỗi, cần sử lý sau
            nextRooms.add(new NextRoom(nodeInstance, i));
        });

        return ActionResult.ok("Lấy các phòng tiếp theo thành công", nextRooms);
    }

    public ActionResult<RoomInstance> selectNextRoom(DungeonInstance dungeonInstance, NodeInstance selectNode) {
        String schemetaId = selectNode.getSchemetas();
        Schemeta schemeta = schemetaService.getSchemeta(schemetaId);

        List<IObjective> objectives = new ArrayList<>();
        List<ObjectiveData> objectiveData = schemeta.getObjectives();
        if (objectiveData != null && !objectiveData.isEmpty()) {
            objectiveData.forEach(oj -> objectives.add(ObjectiveFactory.create(oj)));
        }

        RoomInstance roomInstance = new RoomInstance(selectNode, null, objectives, objectives.isEmpty(), null);

        Optional<Party> party = partyService.getPartyById(dungeonInstance.getParty());

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

                    dungeonFlowService.onRoomCompleted(dungeonInstance, roomInstance);
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
        return ActionResult.ok("Đã chọn room tiếp theo thành công", dungeonInstance.getActiveRoom());
    }

    public ActionResult<DungeonInstance> getInstanceByParty(UUID partyId){
        DungeonInstance instance = instanceService.getInstance(partyId).orElse(null);
        return ActionResult.ok("Got instance of party successfully", instance);
    }

}
