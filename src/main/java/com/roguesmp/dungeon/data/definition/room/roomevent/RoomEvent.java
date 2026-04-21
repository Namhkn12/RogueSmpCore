package com.roguesmp.dungeon.data.definition.room.roomevent;

/**
 * Hook point for room-specific runtime behavior.
 */
public interface RoomEvent {
    void onRoomStart();
    void onRoomPlay();
    void onRoomEnd();
}
