package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.dto.NextRoom;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IDungeonFlowService {
    DungeonInstance onGenerateDungeon(@NotNull String dungeonId,@NotNull Player partyOwner);
    boolean onStartDungeon(DungeonInstance instance);
    List<NextRoom> onOpenDoorGui(DungeonInstance instance);
    RoomInstance onOpenNextRoom(@NotNull DungeonInstance dungeonInstance,@NotNull NodeInstance selectNode);
    void onRoomCompleted(DungeonInstance dungeon, RoomInstance room);
    void onFinishDungeon();
}