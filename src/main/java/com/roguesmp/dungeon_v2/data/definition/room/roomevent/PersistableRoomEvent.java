package com.roguesmp.dungeon_v2.data.definition.room.roomevent;

import java.util.Map;

public interface PersistableRoomEvent {
    Map<String, Object> serialize();
    void deserialize(Map<String, Object> data);
}
