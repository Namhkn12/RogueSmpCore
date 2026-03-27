package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;

public interface IDungeonFlowService {
    void onRoomCompleted(DungeonInstance dungeon, RoomInstance room);
}