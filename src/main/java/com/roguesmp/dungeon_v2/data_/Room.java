package com.roguesmp.dungeon_v2.data_;

import com.roguesmp.dungeon.constant.RoomType;
import com.roguesmp.dungeon_v2.data_.objective_.factory_.ObjectiveConfig;

import java.util.List;

public class Room {
    private String id;
    private RoomType type;
    private String schemetaId;
//    private List<RoomBehavior> behavior;
    private List<ObjectiveConfig> objectives;
}
