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

    //api create new instance dungeon

    //api start an instance dungeon

    //api next room

    //api call when player do event -> check objective
}
