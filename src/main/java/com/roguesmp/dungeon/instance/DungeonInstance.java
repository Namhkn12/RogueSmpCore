package com.roguesmp.dungeon.instance;

import org.bukkit.util.BoundingBox;

import java.util.*;

public class DungeonInstance {
    private UUID uuid;
    private String dungeon;
    private int minRoomToEnd;
    private UUID party;
    private long startTime;
    private RegionInstance region;
    private BoundingBox activeDungeon;
    private Map<UUID, NodeInstance> nodes;      // UUID = NodeInstance.id
    private Map<UUID, Integer> nextRooms;       // UUID match nodes, Integer = slot UI
    private RoomInstance activeRoom;
    private List<RoomInstance> completedRooms;        // ordered, dùng cho checkpoint khi restart
    private boolean isPlaying;
    private int score;
    private long endTime;

    public DungeonInstance() {}

    public DungeonInstance(UUID uuid, String dungeon, UUID party,
                           RegionInstance region, Map<UUID, NodeInstance> nodes, int minRoomToEnd) {
        this.uuid = uuid;
        this.dungeon = dungeon;
        this.party = party;
        this.region = region;
        this.nodes = nodes;
        this.startTime = System.currentTimeMillis();
        this.isPlaying = true;
        this.score = 0;
        this.completedRooms = new ArrayList<>();
        this.activeRoom = null;
        this.nextRooms = new HashMap<>();
        this.minRoomToEnd = minRoomToEnd;
        this.endTime = System.currentTimeMillis() + (20 * 60 * 1000);
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

    public List<RoomInstance> getCompletedRooms() {
        return completedRooms;
    }

    public void setCompletedRooms(List<RoomInstance> completedRooms) {
        this.completedRooms = completedRooms;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMinRoomToEnd() {
        return minRoomToEnd;
    }

    public void setMinRoomToEnd(int minRoomToEnd) {
        this.minRoomToEnd = minRoomToEnd;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getRemainingSeconds() {
        long remaining = endTime - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }
}