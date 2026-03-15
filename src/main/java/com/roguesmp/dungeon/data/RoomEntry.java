package com.roguesmp.dungeon.data;

public class RoomEntry {
    private String file;
    private double weight;

    public RoomEntry() {}

    public RoomEntry(String file, double weight) {
        this.file = file;
        this.weight = weight;
    }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}