package com.roguesmp.dungeon_v2.data.definition.room;

import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;

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

    public Room() {
        this.objectives = new ArrayList<>();
    }

    public Room(String id, RoomType type, String schemetaId, List<ObjectiveConfig> objectives) {
        this.id = id;
        this.type = type;
        this.schemetaId = schemetaId;
        this.objectives = objectives != null ? new ArrayList<>(objectives) : new ArrayList<>();
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
        return objectives;
    }

    public void setObjectives(List<ObjectiveConfig> objectives) {
        this.objectives = objectives != null ? new ArrayList<>(objectives) : new ArrayList<>();
    }
}
