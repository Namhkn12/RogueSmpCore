package com.roguesmp.dungeon.instance;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class DungeonInstance {
    private UUID uuid;
    private String dungeon;
    private UUID party;
    private long startTime;
    private Location location;
    private BoundingBox region;
    private LinkedHashMap<String, NodeInstance> nodes;
    private RoomInstance activeRoom;
    private boolean isPlaying;
    private double score;

    public DungeonInstance(String dungeon, UUID party, long startTime, Location location, boolean isPlaying, double score) {
        this.uuid = UUID.randomUUID();
        this.dungeon = dungeon;
        this.party = party;
        this.startTime = startTime;
        this.location = location;
        this.region = region;
        this.nodes = nodes;
        this.activeRoom = activeRoom;
        this.isPlaying = isPlaying;
        this.score = score;
    }

    public DungeonInstance() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getDungeon() {
        return dungeon;
    }

    public void setDungeon(String dungeon) {
        this.dungeon = dungeon;
    }

    public UUID getParty() {
        return party;
    }

    public void setParty(UUID party) {
        this.party = party;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public BoundingBox getRegion() {
        return region;
    }

    public void setRegion(BoundingBox region) {
        this.region = region;
    }

    public LinkedHashMap<String, NodeInstance> getNodes() {
        return nodes;
    }

    public void setNodes(LinkedHashMap<String, NodeInstance> nodes) {
        this.nodes = nodes;
    }

    public RoomInstance getActiveRoom() {
        return activeRoom;
    }

    public void setActiveRoom(RoomInstance activeRoom) {
        this.activeRoom = activeRoom;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
