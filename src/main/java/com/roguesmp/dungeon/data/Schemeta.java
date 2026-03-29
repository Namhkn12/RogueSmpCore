package com.roguesmp.dungeon.data;

import com.roguesmp.dungeon.objective_.param.ObjectiveData;

import java.util.List;

public class Schemeta {
    private String schemId;
    private String schemName;
    private String schematic;
    //objective
    private List<ObjectiveData> objectives;

    public Schemeta(String schemId, String schemName, String schematic, List<ObjectiveData> objectives) {
        this.schemId = schemId;
        this.schemName = schemName;
        this.schematic = schematic;
        this.objectives = objectives;
    }

    public Schemeta() {
    }

    public String getSchemId() {
        return schemId;
    }

    public void setSchemId(String schemId) {
        this.schemId = schemId;
    }

    public String getSchemName() {
        return schemName;
    }

    public void setSchemName(String schemName) {
        this.schemName = schemName;
    }

    public String getSchematic() {
        return schematic;
    }

    public void setSchematic(String schematic) {
        this.schematic = schematic;
    }

    public List<ObjectiveData> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<ObjectiveData> objectives) {
        this.objectives = objectives;
    }
}
