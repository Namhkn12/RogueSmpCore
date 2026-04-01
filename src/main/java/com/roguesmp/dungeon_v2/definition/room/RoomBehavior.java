package com.roguesmp.dungeon_v2.definition.room;

/**
 * Hook point for room-specific runtime behavior.
 */
public interface RoomBehavior {
    void onRoomStart();
    void onRoomPlay();
    void onRoomEnd();
}
