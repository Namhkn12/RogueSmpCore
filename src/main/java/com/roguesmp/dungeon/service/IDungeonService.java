package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;

import java.util.List;

public interface IDungeonService {

    List<String> rollRoomPool(Dungeon dungeon);

    List<String> rollNextRoomFromPool(List<String> pool, int minium, int completed);

    void chooseRoom(DungeonInstance instance, String chosen);
}