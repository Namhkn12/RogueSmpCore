package com.roguesmp.dungeon.context;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;

public record DungeonContext(Party party, DungeonInstance dungeon, RoomInstance room) {}
