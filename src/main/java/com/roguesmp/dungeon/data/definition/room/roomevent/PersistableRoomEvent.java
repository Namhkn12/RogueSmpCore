package com.roguesmp.dungeon.data.definition.room.roomevent;

import java.util.Map;

public interface PersistableRoomEvent {
    Map<String, Object> serialize();
    void deserialize(Map<String, Object> data);
}
