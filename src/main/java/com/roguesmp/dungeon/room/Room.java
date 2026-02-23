package com.roguesmp.dungeon.room;

import com.roguesmp.dungeon.schemeta.Schemeta;

import java.util.List;
import java.util.UUID;

public class Room {
    private String roomId;
    private String roomName;
    private RoomType roomType;
    private List<String> schemetaList;

    public Room() {
    }

    public Room(String roomId, String roomName, RoomType roomType, List<String> schemetaList) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.schemetaList = schemetaList;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public List<String> getSchemetaList() {
        return schemetaList;
    }

    public void setSchemetaList(List<String> schemetaList) {
        this.schemetaList = schemetaList;
    }
}
