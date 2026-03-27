package com.roguesmp.dungeon.objective_;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;

@FunctionalInterface
public interface RestoreObjCallBack {
    void provide(DungeonInstance instance, RoomInstance room, IObjective objective);
}
