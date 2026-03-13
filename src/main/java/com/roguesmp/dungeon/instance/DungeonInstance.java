package com.roguesmp.dungeon.instance;

import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DungeonInstance {
    private UUID uuid;
    private String dungeon;
    private UUID party;
    private long startTime;
    private RegionInstance region;
    private BoundingBox activeDungeon;
    private Map<UUID, NodeInstance> nodes;      // UUID = NodeInstance.id
    private Map<UUID, Integer> nextRooms;       // UUID match nodes, Integer = slot UI
    private RoomInstance activeRoom;
    private List<String> completedRooms;        // ordered, dùng cho checkpoint khi restart
    private boolean isPlaying;
    private double score;

    public DungeonInstance() {}

    public DungeonInstance(UUID uuid, String dungeon, UUID party,
                           RegionInstance region, Map<UUID, NodeInstance> nodes) {
        this.uuid = uuid;
        this.dungeon = dungeon;
        this.party = party;
        this.region = region;
        this.nodes = nodes;
        this.startTime = System.currentTimeMillis();
        this.isPlaying = true;
        this.score = 0;
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

    public RegionInstance getRegion() {
        return region;
    }

    public void setRegion(RegionInstance region) {
        this.region = region;
    }

    public BoundingBox getActiveDungeon() {
        return activeDungeon;
    }

    public void setActiveDungeon(BoundingBox activeDungeon) {
        this.activeDungeon = activeDungeon;
    }

    public Map<UUID, NodeInstance> getNodes() {
        return nodes;
    }

    public void setNodes(Map<UUID, NodeInstance> nodes) {
        this.nodes = nodes;
    }

    public Map<UUID, Integer> getNextRooms() {
        return nextRooms;
    }

    public void setNextRooms(Map<UUID, Integer> nextRooms) {
        this.nextRooms = nextRooms;
    }

    public RoomInstance getActiveRoom() {
        return activeRoom;
    }

    public void setActiveRoom(RoomInstance activeRoom) {
        this.activeRoom = activeRoom;
    }

    public List<String> getCompletedRooms() {
        return completedRooms;
    }

    public void setCompletedRooms(List<String> completedRooms) {
        this.completedRooms = completedRooms;
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