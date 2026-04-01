package com.roguesmp.dungeon_v2.data.definition.room.roomevent;

/**
 * Hook point for room-specific runtime behavior.
 */
public interface RoomEvent {
    void onRoomStart();
    void onRoomPlay();
    void onRoomEnd();
}
