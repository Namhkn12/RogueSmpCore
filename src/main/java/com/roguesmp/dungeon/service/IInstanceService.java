package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.UUID;

public interface IInstanceService {
    DungeonInstance createDungeonInstance(String dungeon, UUID party, Location location);
}
