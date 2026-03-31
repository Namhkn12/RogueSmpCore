package com.roguesmp.dungeon_v2.data_;

import java.util.List;

public class RoomPool {
    private String id;
    private String icon;
    private String name;
    /*Change to take room in this pool*/
    private double weight;
    /*Min room to get*/
    private int min;
    /*Max room to get*/
    private int max;
    private List<RoomEntry> rooms;
}

