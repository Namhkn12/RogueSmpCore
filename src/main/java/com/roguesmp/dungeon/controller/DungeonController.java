package com.roguesmp.dungeon.controller;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.dto.NextRoom;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.objective.IObjective;
import com.roguesmp.dungeon.objective.ObjectiveData;
import com.roguesmp.dungeon.objective.ObjectiveFactory;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.utils.DungeonEcho;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class DungeonController {

    private final IPartyService partyService;
    private final IDungeonService dungeonService;
    private final ISchemetaService schemetaService;
    private final IInstanceService instanceService;
    private final IRegionService regionService;

    public DungeonController(IPartyService partyService, IDungeonService dungeonService,
                             ISchemetaService schemetaService, IInstanceService instanceService,
                             IRegionService regionService) {
        this.partyService = partyService;
        this.dungeonService = dungeonService;
        this.schemetaService = schemetaService;
        this.instanceService = instanceService;
        this.regionService = regionService;
    }

    public ActionResult<DungeonInstance> generateDungeon(String template, Player player) {
        if (!partyService.isOwner(player))
            return ActionResult.failed("You are not the party owner");
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(template);
        if (dungeon.isEmpty()) return ActionResult.failed("Cannot find dungeon template");

        Optional<Party> party = partyService.getPartyByPlayer(player);
        Optional<Region> region = regionService.acquireRegion();

        if (party.isEmpty())  return ActionResult.failed("Party not found");
        if (region.isEmpty()) return ActionResult.failed("No region available");

        // Kiểm tra party chưa có dungeon đang chạy
        if (instanceService.hasActiveInstance(party.get().getPartyId()))
            return ActionResult.failed("Your party already has an active dungeon");

        RegionInstance regionInstance = new RegionInstance(
                region.get().getId(),
                region.get().getWorldName(),
                region.get().getRegionPoint()
        );

        DungeonInstance instance = instanceService.createDungeonInstance(
                dungeon.get(),
                party.get().getPartyId(),
                regionInstance
        );

        return ActionResult.ok("Dungeon instance create successfully", instance);
    }

    public ActionResult<Void> startDungeon(DungeonInstance instance) {

        RegionInstance region = instance.getRegion();
        UUID partyId = instance.getParty();

        Map<UUID, NodeInstance> nodes = instance.getNodes();
        if (nodes == null || nodes.isEmpty())
            return ActionResult.failed("Dungeon has no rooms");

        NodeInstance startNode = nodes.values().stream()
                .filter(n -> "start".equals(n.getNodeKey()))
                .findFirst()
                .orElse(null);
        if (startNode == null)
            return ActionResult.failed("Start node not found");

        // Paste schematic
        try {
            schemetaService.pasteSchematic(startNode.getSchemetas(), region.getLocation());
        } catch (Exception e) {
            RogueSmpCore.getInstance().getLogger().severe(
                    "[DungeonController] Failed to paste schematic: " + startNode.getSchemetas()
            );
            return ActionResult.failed("Failed to build dungeon room");
        }

        // Set activeRoom — fix null check ở selectNextRoom sau này
        RoomInstance startRoom = new RoomInstance(startNode, null, null, true, null);
        instance.setActiveRoom(startRoom);

        // Remove start node khỏi pool rồi roll nextRooms
        nodes.remove(startNode.getId());
        instanceService.rollNextRooms(instance);

        // Teleport party
        Optional<Party> party = partyService.getPartyById(partyId);
        if (party.isPresent()) {
            for (UUID memberId : party.get().getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.teleport(region.getLocation());
                    DungeonEcho.success(member, "Dungeon bắt đầu! Chúc may mắn.");
                }
            }
        }

        instanceService.saveInstance(partyId);
        return ActionResult.ok("Dungeon start successfully");
    }

    public ActionResult<List<NextRoom>> getNextRooms(DungeonInstance instance) {
        Map<UUID, Integer> nextRoomInstance = instance.getNextRooms();
        Map<UUID, NodeInstance> nodePools = instance.getNodes();

        if (nextRoomInstance == null || nextRoomInstance.isEmpty())
            return ActionResult.ok("Đã đi hết phòng dungeon", null);

        List<NextRoom> nextRooms = new ArrayList<>();
        nextRoomInstance.forEach((ri, i) -> {
            NodeInstance nodeInstance = nodePools.get(ri);
            if (nodeInstance == null) return; // fallback: bỏ qua node lỗi, cần sử lý sau
            nextRooms.add(new NextRoom(nodeInstance, i));
        });

        return ActionResult.ok("Lấy các phòng tiếp theo thành công", nextRooms);
    }

    public ActionResult<RoomInstance> selectNextRoom(DungeonInstance dungeonInstance, NodeInstance selectNode){
        String schemetaId = selectNode.getSchemetas();
        Schemeta schemeta = schemetaService.getSchemeta(schemetaId);
        List<IObjective> objectives = new ArrayList<>();
        List<ObjectiveData> objectiveData = schemeta.getObjectives();
        if(objectiveData != null && !objectiveData.isEmpty()){
            objectiveData.forEach(oj -> {
                IObjective objective = ObjectiveFactory.create(oj);
                objectives.add(objective);
            });
        }
        RoomInstance roomInstance = new RoomInstance(selectNode, null, objectives, objectives.isEmpty(), null);
        dungeonInstance.getCompletedRooms().add(dungeonInstance.getActiveRoom());
        dungeonInstance.setActiveRoom(roomInstance);
        dungeonInstance.getNodes().remove(selectNode.getId());
        instanceService.rollNextRooms(dungeonInstance);
        return ActionResult.ok("Đã chọn room tiếp theo thành công", dungeonInstance.getActiveRoom());
    }

    public ActionResult<DungeonInstance> getInstanceByParty(UUID partyId){
        DungeonInstance instance = instanceService.getInstance(partyId).orElse(null);
        return ActionResult.ok("Got instance of party successfully", instance);
    }
}