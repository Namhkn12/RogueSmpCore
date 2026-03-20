package com.roguesmp.dungeon.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.adapter.LocationAdapter;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
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

    // -------------------------------------------------------------------------
    // API: Tạo instance dungeon
    // -------------------------------------------------------------------------

    public ControllerResponse<DungeonInstance> generateDungeon(String template, Player player) {
        if (!partyService.isOwner(player))
            return ControllerResponse.failure("You are not the party owner");
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(template);
        if (dungeon.isEmpty()) return ControllerResponse.failure("Cannot find dungeon template");

        Optional<Party> party = partyService.getPartyByPlayer(player);
        Optional<Region> region = regionService.acquireRegion();

        if (party.isEmpty())  return ControllerResponse.failure("Party not found");
        if (region.isEmpty()) return ControllerResponse.failure("No region available");

        // Kiểm tra party chưa có dungeon đang chạy
        if (instanceService.hasActiveInstance(party.get().getPartyId()))
            return ControllerResponse.failure("Your party already has an active dungeon");

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

        return ControllerResponse.success("GG", instance);
    }

    // -------------------------------------------------------------------------
    // API: Bắt đầu dungeon — paste start room rồi tp party
    // -------------------------------------------------------------------------

    public ControllerResponse<Void> startDungeon(DungeonInstance instance) {

        RegionInstance region = instance.getRegion();
        UUID partyId = instance.getParty();

        Map<UUID, NodeInstance> nodes = instance.getNodes();
        if (nodes == null || nodes.isEmpty())
            return ControllerResponse.failure("Dungeon has no rooms");

        NodeInstance startNode = nodes.values().stream()
                .filter(n -> "start".equals(n.getNodeKey()))
                .findFirst()
                .orElse(null);
        if (startNode == null)
            return ControllerResponse.failure("Start node not found");

        // Paste schematic
        try {
            schemetaService.pasteSchematic(startNode.getSchemetas(), region.getLocation());
        } catch (Exception e) {
            RogueSmpCore.getInstance().getLogger().severe(
                    "[DungeonController] Failed to paste schematic: " + startNode.getSchemetas()
            );
            return ControllerResponse.failure("Failed to build dungeon room");
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
                    member.sendMessage("§aDungeon bắt đầu! Chúc may mắn.");
                }
            }
        }

        instanceService.saveInstance(partyId);
        return ControllerResponse.success(null);
    }

    // -------------------------------------------------------------------------
    // API: Next room (TODO)
    // -------------------------------------------------------------------------
    public ControllerResponse<List<NextRoom>> getNextRooms(DungeonInstance instance) {
        Map<UUID, Integer> nextRoomInstance = instance.getNextRooms();
        Map<UUID, NodeInstance> nodePools = instance.getNodes();

        if (nextRoomInstance == null || nextRoomInstance.isEmpty())
            return ControllerResponse.success("Đã đi hết phòng dungeon", null);

        List<NextRoom> nextRooms = new ArrayList<>();
        nextRoomInstance.forEach((ri, i) -> {
            NodeInstance nodeInstance = nodePools.get(ri);
            if (nodeInstance == null) return; // fallback: bỏ qua node lỗi, cần sử lý sau
            nextRooms.add(new NextRoom(nodeInstance, i));
        });

        return ControllerResponse.success("Lấy các phòng tiếp theo thành công", nextRooms);
    }

    public ControllerResponse<RoomInstance> selectNextRoom(DungeonInstance dungeonInstance, NodeInstance selectNode){
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
        return ControllerResponse.success("Đã chọn room tiếp theo thành công", dungeonInstance.getActiveRoom());
    }

    // -------------------------------------------------------------------------
    // API: Check objective (TODO)
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // API: Get dungeonInstance by Player (TODO)
    // -------------------------------------------------------------------------
    public ControllerResponse<DungeonInstance> getInstanceByParty(UUID partyId){
        DungeonInstance instance = instanceService.getInstance(partyId).orElse(null);
        return ControllerResponse.success("Lấy instance theo party thành công", instance);
    }

    // ---- TEMP DEBUG ----
    private void debugSaveInstance(DungeonInstance instance) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .create();
        File file = new File(
                RogueSmpCore.getInstance().getDataFolder(),
                "debug_instance_" + instance.getUuid() + ".json"
        );
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
            RogueSmpCore.getInstance().getLogger().info(
                    "[DEBUG] Instance saved to: " + file.getAbsolutePath()
            );
        } catch (IOException e) {
            RogueSmpCore.getInstance().getLogger().severe(
                    "[DEBUG] Failed to save instance: " + e.getMessage()
            );
        }
    }
    // ---- END TEMP DEBUG ----
}