package com.roguesmp.dungeon.instance;

import com.roguesmp.dungeon.objective.IObjective;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class RoomInstance {
    private String name; // tên
    private BoundingBox roomBounds; // bọc lại room
    private IObjective objective; // mục tiêu đang thực hiện
    private boolean completed; // trạng thái của room
    private Location checkpoint; // check point nếu có

}
