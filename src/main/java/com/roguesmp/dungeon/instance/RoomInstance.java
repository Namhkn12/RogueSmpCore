package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.objective.IObjective;
import org.bukkit.util.BoundingBox;

public class RoomInstance {
    private String name;
    private BoundingBox roomBounds;
    private IObjective objective;
    private boolean completed;

    public RoomInstance(String name, BoundingBox roomBounds, IObjective objective, boolean completed) {
        this.name = name;
        this.roomBounds = roomBounds;
        this.objective = objective;
        this.completed = completed;
    }

    public RoomInstance() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BoundingBox getRoomBounds() {
        return roomBounds;
    }

    public void setRoomBounds(BoundingBox roomBounds) {
        this.roomBounds = roomBounds;
    }

    public IObjective getObjective() {
        return objective;
    }

    public void setObjective(IObjective objective) {
        this.objective = objective;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
