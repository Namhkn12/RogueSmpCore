package com.roguesmp.dungeon.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.adapter.LocationAdapter;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;
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
        if (dungeonService.getDungeonById(template).isEmpty())
            return ControllerResponse.failure("Cannot find dungeon template");

        Optional<Party> party = partyService.getPartyByPlayer(player);
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(template);
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
                dungeon.get().getDgId(),
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

        // Lấy NodeInstance đầu tiên (start node)
        Map<UUID, NodeInstance> nodes = instance.getNodes();
        if (nodes == null || nodes.isEmpty())
            return ControllerResponse.failure("Dungeon has no rooms");

        NodeInstance startNode = nodes.values().iterator().next();
        if (startNode.getSchemetas() == null || startNode.getSchemetas().isEmpty())
            return ControllerResponse.failure("Start room has no schematic");

        String schemetaId = startNode.getSchemetas().get(0);
        Location pasteLocation = region.getLocation();

        // Paste schematic vào region
        try {
            schemetaService.pasteSchematic(schemetaId, pasteLocation);
        } catch (Exception e) {
            RogueSmpCore.getInstance().getLogger().severe(
                    "[DungeonController] Failed to paste schematic: " + schemetaId + "|" + pasteLocation
            );
            return ControllerResponse.failure("Failed to build dungeon room");
        }

        // Xóa start node khỏi queue vì đã dùng
        nodes.remove(startNode.getId());

        // Tp tất cả member trong party đến location region
        Optional<Party> party = partyService.getPartyById(instance.getParty());

        if (party.isPresent()) {
            for (UUID memberId : party.get().getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.teleport(pasteLocation);
                    member.sendMessage("§aDungeon bắt đầu! Chúc may mắn.");
                }
            }
        }

        // Lưu lại state sau khi remove start node
        instanceService.saveInstance(partyId);

        return ControllerResponse.success(null);
    }

    // -------------------------------------------------------------------------
    // API: Next room (TODO)
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // API: Check objective (TODO)
    // -------------------------------------------------------------------------

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