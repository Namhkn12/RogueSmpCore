package com.roguesmp.dungeon_v2.definition.room;

import java.util.ArrayList;
import java.util.List;

/**
 * Weighted group of rooms used by the generator.
 */
public class RoomPool {
    private String id;
    private String icon;
    private String name;
    private double weight;
    private int min;
    private int max;
    private List<RoomEntry> rooms;

    public RoomPool() {
        this.rooms = new ArrayList<>();
    }

    public RoomPool(String id, String icon, String name, double weight, int min, int max, List<RoomEntry> rooms) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.weight = weight;
        this.min = min;
        this.max = max;
        this.rooms = rooms != null ? new ArrayList<>(rooms) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public List<RoomEntry> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomEntry> rooms) {
        this.rooms = rooms != null ? new ArrayList<>(rooms) : new ArrayList<>();
    }
}
