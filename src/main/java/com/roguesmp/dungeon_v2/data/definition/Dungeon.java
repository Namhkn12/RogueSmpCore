package com.roguesmp.dungeon_v2.data.definition;

import com.roguesmp.dungeon_v2.data.definition.room.RoomPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable-by-convention definition of a dungeon template.
 * Runtime progress must live outside this class.
 */
public class Dungeon {
    private String id;
    private String name;
    private String description;
    private String lootTableId;
    private int playTime; // minutes
    private boolean active;
    private int minimumRooms; // rooms required before boss access
    private List<RoomPool> pools;

    public Dungeon() {
        this.pools = new ArrayList<>();
    }

    public Dungeon(String id, String name, String description, String lootTableId, int playTime,
                   boolean active, int minimumRooms, List<RoomPool> pools) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.lootTableId = lootTableId;
        this.playTime = playTime;
        this.active = active;
        this.minimumRooms = minimumRooms;
        this.pools = pools != null ? new ArrayList<>(pools) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLootTableId() {
        return lootTableId;
    }

    public void setLootTableId(String lootTableId) {
        this.lootTableId = lootTableId;
    }

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMinimumRooms() {
        return minimumRooms;
    }

    public void setMinimumRooms(int minimumRooms) {
        this.minimumRooms = minimumRooms;
    }

    public List<RoomPool> getPools() {
        return pools;
    }

    public void setPools(List<RoomPool> pools) {
        this.pools = pools != null ? new ArrayList<>(pools) : new ArrayList<>();
    }
}
