package com.roguesmp.dungeon_v2.data.definition;

/**
 * Points to a schematic asset that can be placed into a room instance.
 */
public class Schemeta {
    private String id;
    private String name;
    private String schematic;

    public Schemeta() {
    }

    public Schemeta(String id, String name, String schematic) {
        this.id = id;
        this.name = name;
        this.schematic = schematic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchematic() {
        return schematic;
    }

    public void setSchematic(String schematic) {
        this.schematic = schematic;
    }
}
