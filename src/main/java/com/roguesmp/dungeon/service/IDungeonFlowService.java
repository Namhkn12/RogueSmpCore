package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IDungeonFlowService {
    DungeonInstance onGenerateDungeon(@NotNull String dungeonId,@NotNull Player partyOwner);
    boolean onStartDungeon(DungeonInstance instance);
    void onOpenNextRoom();
    void onRoomCompleted(DungeonInstance dungeon, RoomInstance room);
    void onFinishDungeon();
}