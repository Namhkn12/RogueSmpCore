package com.roguesmp.dungeon.objective;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;

@FunctionalInterface
public interface ObjectiveRestoreCallback {
    void onObjectiveComplete(DungeonInstance instance, RoomInstance room, IObjective objective);
}
