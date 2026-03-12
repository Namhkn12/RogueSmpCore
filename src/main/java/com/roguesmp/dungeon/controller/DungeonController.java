package com.roguesmp.dungeon.controller;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.instance.DungeonInstance;
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
import com.roguesmp.dungeon.service.IDungeonService;
import com.roguesmp.dungeon.service.IInstanceService;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DungeonController {

    private final IPartyService partyService;
    private final IDungeonService dungeonService;
    private final ISchemetaService schemetaService;
    private final IInstanceService instanceService;

    //api create new instance dungeon
    public ControllerResponse<DungeonInstance> generateDungeon(String template, Player player){
        if(!partyService.isOwner(player)) return ControllerResponse.failure("You do not in a party");
        if(dungeonService.getDungeonById(template).isEmpty()) return ControllerResponse.failure("Cannot find dungeon template");
        Optional<Party> party = partyService.getPartyByPlayer(player);
        Optional<Dungeon> dungeon = dungeonService.getDungeonById(template);

        instanceService.createDungeonInstance(dungeon.get().getDgName(), party.get().getPartyId(), )

    }

    //api start an instance dungeon

    //api next room

    //api call when player do event -> check objective
}
