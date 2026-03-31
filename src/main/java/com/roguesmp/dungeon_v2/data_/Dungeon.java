package com.roguesmp.dungeon_v2.data_;

import java.util.List;

public class Dungeon {
    private String id;
    private String name;
    private String description;
    private String lootTableId;
    private int playTime; //minute
    private boolean active;
    private int minium; //minium room needed to be clear before meet boss room
    private List<RoomPool> pools;

}
