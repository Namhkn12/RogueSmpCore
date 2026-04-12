package com.roguesmp.dungeon_v2.data.definition.room.roomevent;

public interface RoomEventCallback {
    void callback(RoomEvent event, RoomEventPhase phase);
}
