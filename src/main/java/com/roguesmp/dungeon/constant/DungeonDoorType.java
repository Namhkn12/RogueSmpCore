package com.roguesmp.dungeon.constant;

public enum DungeonDoorType {
    NEXTDOOR("next_door", "Mark that this door is the normal door"),
    ENDDOOR("end_door", "Mark that this door is the finish door");

    private String type;
    private String description;

    DungeonDoorType(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
