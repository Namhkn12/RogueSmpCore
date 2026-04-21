package com.roguesmp.dungeon.data.definition.room.roomevent;

public interface RoomEventCallback {
    void callback(RoomEvent event, RoomEventPhase phase);
}
