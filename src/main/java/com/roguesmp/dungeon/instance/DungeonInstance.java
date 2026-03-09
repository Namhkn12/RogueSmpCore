package com.roguesmp.dungeon.instance;

import java.util.List;
import java.util.UUID;

public class DungeonInstance {
    private UUID uuid;
    private long startTime;
    private UUID partyId;
    private String dungeonId;
    private UUID regionId;
    private List<String> roomIds;
    private boolean status;

    public DungeonInstance() {
    }

    public DungeonInstance(UUID uuid, long startTime, UUID partyId, String dungeonId, UUID regionId, boolean status) {
        this.uuid = uuid;
        this.startTime = startTime;
        this.partyId = partyId;
        this.dungeonId = dungeonId;
        this.regionId = regionId;
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

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public void setRegionId(UUID regionId) {
        this.regionId = regionId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
