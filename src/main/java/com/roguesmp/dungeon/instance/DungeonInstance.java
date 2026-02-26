package com.roguesmp.dungeon.instance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DungeonInstance {
    private UUID uuid;
    private long startTime;
    private String partyId;
    private String dungeonId;
    private String regionId;
    private List<String> roomIds;
    private boolean status;

    public DungeonInstance() {
    }

    public DungeonInstance(UUID uuid, long startTime, String partyId, String dungeonId, String regionId, List<String> roomIds, boolean status) {
        this.uuid = uuid;
        this.startTime = startTime;
        this.partyId = partyId;
        this.dungeonId = dungeonId;
        this.regionId = regionId;
        this.roomIds = roomIds;
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public List<String> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<String> roomIds) {
        this.roomIds = roomIds;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
