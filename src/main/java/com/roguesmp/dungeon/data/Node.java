package com.roguesmp.dungeon.data;

import java.util.List;

public class Node {
    private String key;
    private String name;
    private String icon;
    private double chance;
    private int number;
    private List<RoomEntry> rooms;

    public Node() {}

    public Node(String key, String name, String icon, double chance, int number, List<RoomEntry> rooms) {
        this.key = key;
        this.name = name;
        this.icon = icon;
        this.chance = chance;
        this.number = number;
        this.rooms = rooms;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public List<RoomEntry> getRooms() { return rooms; }
    public void setRooms(List<RoomEntry> rooms) { this.rooms = rooms; }
}