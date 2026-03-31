package com.roguesmp.dungeon_v2.data_.runtime_;

import org.bukkit.Location;

import java.util.UUID;

public class RegionInstance {
    private UUID id; // id
    private String world; // tên thế giới liên quan cũng key để kết nối đến wolrd manager
    private Location location; // vị trí region

    public RegionInstance(UUID id, String world, Location location) {
        this.id = id;
        this.world = world;
        this.location = location;
    }

    public RegionInstance() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}