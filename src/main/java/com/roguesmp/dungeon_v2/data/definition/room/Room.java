package com.roguesmp.dungeon_v2.data.definition.room;

import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Static room definition used by dungeon generation.
 */
public class Room {
    private String id;
    private RoomType type;
    private String schemetaId;
    private List<ObjectiveConfig> objectives;
    private List<RoomEventConfig> roomEvents;

    public Room() {
        this.objectives = new ArrayList<>();
        this.roomEvents = new ArrayList<>();
    }

    public Room(String id, RoomType type, String schemetaId, List<ObjectiveConfig> objectives, List<RoomEventConfig> roomEvents) {
        this.id = id;
        this.type = type;
        this.schemetaId = schemetaId;
        this.objectives = objectives != null ? new ArrayList<>(objectives) : new ArrayList<>();
        this.roomEvents = roomEvents != null ? new ArrayList<>(roomEvents) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public String getSchemetaId() {
        return schemetaId;
    }

    public void setSchemetaId(String schemetaId) {
        this.schemetaId = schemetaId;
    }

    public List<ObjectiveConfig> getObjectives() {
        if (objectives == null) {
            objectives = new ArrayList<>();
        }
        return objectives;
    }

    public void setObjectives(List<ObjectiveConfig> objectives) {
        this.objectives = objectives != null ? new ArrayList<>(objectives) : new ArrayList<>();
    }

    public List<RoomEventConfig> getRoomEvents() {
        if (roomEvents == null) {
            roomEvents = new ArrayList<>();
        }
        return roomEvents;
    }

    public void setRoomEvents(List<RoomEventConfig> roomEvents) {
        this.roomEvents = roomEvents != null ? new ArrayList<>(roomEvents) : new ArrayList<>();
    }
}
