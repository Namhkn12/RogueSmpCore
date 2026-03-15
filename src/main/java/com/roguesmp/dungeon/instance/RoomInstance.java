package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.objective.IObjective;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.List;

public class RoomInstance {
    private NodeInstance activeNode;//
    private BoundingBox roomBounds; // bọc lại room
    private List<IObjective> objective; // mục tiêu đang thực hiện
    private boolean completed; // trạng thái của room
    private Location checkpoint; // check point nếu có

    public RoomInstance() {
    }

    public RoomInstance(NodeInstance activeNode, BoundingBox roomBounds, List<IObjective> objective, boolean completed, Location checkpoint) {
        this.activeNode = activeNode;
        this.roomBounds = roomBounds;
        this.objective = objective;
        this.completed = completed;
        this.checkpoint = checkpoint;
    }

    public NodeInstance getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(NodeInstance activeNode) {
        this.activeNode = activeNode;
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
