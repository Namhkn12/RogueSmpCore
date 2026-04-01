package com.roguesmp.dungeon_v2.data.definition.room;

/**
 * Weighted reference to a room definition inside a room pool.
 */
public class RoomEntry {
    private String roomId;
    private double weight;

    public RoomEntry() {
    }

    public RoomEntry(String roomId, double weight) {
        this.roomId = roomId;
        this.weight = weight;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
