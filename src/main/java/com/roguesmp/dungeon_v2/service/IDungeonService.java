package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.dto.DungeonConfig;
import org.bukkit.entity.Player;

import java.util.List;

public interface IDungeonService {

    List<String> rollRoomPool(Dungeon dungeon);

    List<String> rollNextRoomFromPool(List<String> pool, int minium, int completed);

    void chooseRoom(DungeonInstance instance, String chosen);
}