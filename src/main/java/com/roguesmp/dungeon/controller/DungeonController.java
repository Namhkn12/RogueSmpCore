package com.roguesmp.dungeon.controller;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.DungeonInstanceManager;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.manager.DungeonWorldManager;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.manager.RegionManager;
import com.roguesmp.dungeon.data.Room;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon.constraint.RoomType;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.roguesmp.dungeon.service.impl.PartyService;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DungeonController {
    // key: partyId, value: dungeonInstanceId
    private final Map<String, String> dungeon = new HashMap<>();

    private final ISchemetaService schemetaService;

    private static DungeonController INSTANCE = null;
    private final RogueSmpCore plugin;
    private final DungeonInstanceManager dungeonInstanceManager;
    private final DungeonWorldManager dungeonWorldManager;
    private final RoomManager roomManager;
    private final RegionManager regionManager;
    private PartyManager partyManager;
    private SchemetaManager schemetaManager;
    private DungeonManager dungeonManager;

//    public static void init(){
//        if(INSTANCE == null){
//            INSTANCE = new DungeonController();
//        }
//    }

    public static DungeonController getInstance(){
        return INSTANCE;
    }

    public DungeonController(ISchemetaService schemetaService, RogueSmpCore plugin, DungeonInstanceManager dungeonInstanceManager, DungeonWorldManager dungeonWorldManager, RoomManager roomManager, RegionManager regionManager, PartyManager partyManager, SchemetaManager schemetaManager, DungeonManager dungeonManager) {
        this.schemetaService = schemetaService;
        this.plugin = plugin;
        this.dungeonInstanceManager = dungeonInstanceManager;
        this.dungeonWorldManager = dungeonWorldManager;
        this.roomManager = roomManager;
        this.regionManager = regionManager;
        this.partyManager = partyManager;
        this.schemetaManager = schemetaManager;
        this.dungeonManager = dungeonManager;
    }

    //find dungeon with party id
    public void startDungeonInstance(UUID partyId, String dungeonId){
        Region region = dungeonWorldManager.getRegionSlotForDungeon();
        dungeonInstanceManager.createDungeonInstance(partyId, dungeonId, region.getRegionId());
        //start dungeon
        //-> create region
        Dungeon selectedDungeon = dungeonManager.getById(dungeonId);
        if(selectedDungeon == null) {
            // cannot find dungeon
            return;
        }
        //-> paste room ( start room )
        Room startRoom = selectedDungeon.getDgRooms()
                .stream()
                .filter(r -> r.getRoomType() == RoomType.START)
                .findFirst()
                .orElse(null);

        this.buildDungeonRoom(region.getRegionPoint(), startRoom);

        //-> tp party to room
        this.teleportPartyToDungeon(partyId, region.getRegionPoint());
    }

    //create dungeon with party

    //remove playing dungeon

    public void teleportPartyToDungeon(UUID partyId, Location location){
        Party party = partyManager.getParty(partyId);
        party.getMembers().forEach(mem -> {
            Player player = Bukkit.getPlayer(mem);
            if(player != null && player.isOnline()){
                player.teleport(location);
            }
        });
    }

    public void buildDungeonRoom(Location location, Room room) {
        try {

            String schemId = getRandomSchemeta(room);
            Schemeta schemeta = schemetaManager.get(schemId);

            if (schemeta == null) return;

            String schematic = schemeta.getSchematic();

            File schemFile = new File(plugin.getDataFolder(), schematic);

            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) return;

            Clipboard clipboard;

            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                clipboard = reader.read();
            }

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(
                                location.getBlockX(),
                                location.getBlockY(),
                                location.getBlockZ()
                        ))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRandomSchemeta(Room room){
        List<String> list = room.getSchemetaList();
        if(list == null || list.isEmpty()) return null;

        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }


}
