package com.roguesmp.dungeon;

import com.roguesmp.dungeon.room.Room;

import java.util.List;

public class Dungeon {
    private String dgId;
    private String dgName;
    private String dgDescription;
    private List<String> dgRooms;

    public Dungeon() {
    }

    public Dungeon(String dgId, String dgName, String dgDescription, List<String> dgRooms) {
        this.dgId = dgId;
        this.dgName = dgName;
        this.dgDescription = dgDescription;
        this.dgRooms = dgRooms;
    }

    public String getDgId() {
        return dgId;
    }

    public void setDgId(String dgId) {
        this.dgId = dgId;
    }

    public String getDgName() {
        return dgName;
    }

    public void setDgName(String dgName) {
        this.dgName = dgName;
    }

    public String getDgDescription() {
        return dgDescription;
    }

    public void setDgDescription(String dgDescription) {
        this.dgDescription = dgDescription;
    }

    public List<String> getDgRooms() {
        return dgRooms;
    }

    public void setDgRooms(List<String> dgRooms) {
        this.dgRooms = dgRooms;
    }
}
