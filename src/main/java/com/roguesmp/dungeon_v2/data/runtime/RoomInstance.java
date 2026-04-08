package com.roguesmp.dungeon_v2.data.runtime;

import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon_v2.helper.SerializableBounds;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runtime snapshot of one room inside an active dungeon run.
 * roomId references the static Room template for type/name lookup.
 */
public class RoomInstance {

    private String roomId;
    private SerializableBounds bounds;
    private SerializableLocation door;
    private boolean completed;
    private List<Map<String, Object>> objectiveStates;
    private List<Map<String, Object>> roomEventStates;

    // transient runtime-only references restored from serialized states
    private transient List<IObjective> activeObjectives;
    private transient List<RoomEvent> activeRoomEvents;

    public RoomInstance() {
        this.objectiveStates = new ArrayList<>();
        this.roomEventStates = new ArrayList<>();
        this.activeObjectives = new ArrayList<>();
        this.activeRoomEvents = new ArrayList<>();
    }

    public RoomInstance(String roomId, SerializableBounds bounds) {
        this.roomId = roomId;
        this.bounds = bounds;
        this.completed = false;
        this.objectiveStates = new ArrayList<>();
        this.roomEventStates = new ArrayList<>();
        this.activeObjectives = new ArrayList<>();
        this.activeRoomEvents = new ArrayList<>();
    }

    public boolean isInside(double x, double y, double z) {
        return bounds != null && bounds.contains(x, y, z);
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public SerializableBounds getBounds() { return bounds; }
    public void setBounds(SerializableBounds bounds) { this.bounds = bounds; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public List<Map<String, Object>> getObjectiveStates() {
        if (objectiveStates == null) {
            objectiveStates = new ArrayList<>();
        }
        return objectiveStates;
    }
    public void setObjectiveStates(List<Map<String, Object>> objectiveStates) {
        this.objectiveStates = objectiveStates;
    }

    public List<IObjective> getActiveObjectives() {
        if (activeObjectives == null) {
            activeObjectives = new ArrayList<>();
        }
        return activeObjectives;
    }
    public void setActiveObjectives(List<IObjective> activeObjectives) {
        this.activeObjectives = activeObjectives;
    }

    public List<Map<String, Object>> getRoomEventStates() {
        if (roomEventStates == null) {
            roomEventStates = new ArrayList<>();
        }
        return roomEventStates;
    }

    public void setRoomEventStates(List<Map<String, Object>> roomEventStates) {
        this.roomEventStates = roomEventStates;
    }

    public List<RoomEvent> getActiveRoomEvents() {
        if (activeRoomEvents == null) {
            activeRoomEvents = new ArrayList<>();
        }
        return activeRoomEvents;
    }

    public void setActiveRoomEvents(List<RoomEvent> activeRoomEvents) {
        this.activeRoomEvents = activeRoomEvents;
    }

    public SerializableLocation getDoor() {
        return door;
    }

    public void setDoor(SerializableLocation door) {
        this.door = door;
    }
}
