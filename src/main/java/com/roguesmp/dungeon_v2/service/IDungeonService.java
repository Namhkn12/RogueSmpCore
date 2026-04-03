package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.dto.DungeonConfig;
import org.bukkit.entity.Player;

import java.util.List;

public interface IDungeonService {

    DungeonInstance startDungeon(Player player, DungeonConfig config);

    List<String> rollNextRooms(DungeonInstance instance);

    void chooseRoom(DungeonInstance instance, String chosen);
}