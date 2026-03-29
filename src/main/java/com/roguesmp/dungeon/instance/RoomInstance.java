package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.List;

public class RoomInstance {
    private NodeInstance node;//
    private BoundingBox roomBounds; // bọc lại room
    private List<IObjective> objective; // mục tiêu đang thực hiện
    private boolean completed; // trạng thái của room
    private Location checkpoint; // check point nếu có

    public RoomInstance() {
    }

    public RoomInstance(NodeInstance node, BoundingBox roomBounds, List<IObjective> objective, boolean completed, Location checkpoint) {
        this.node = node;
        this.roomBounds = roomBounds;
        this.objective = objective;
        this.completed = completed;
        this.checkpoint = checkpoint;
    }

    public NodeInstance getNode() {
        return node;
    }

    public void setNode(NodeInstance node) {
        this.node = node;
    }

    public BoundingBox getRoomBounds() {
        return roomBounds;
    }

    public void setRoomBounds(BoundingBox roomBounds) {
        this.roomBounds = roomBounds;
    }

    public List<IObjective> getObjective() {
        return objective;
    }

    public void setObjective(List<IObjective> objective) {
        this.objective = objective;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Location getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(Location checkpoint) {
        this.checkpoint = checkpoint;
    }
}
