package com.roguesmp.dungeon.data;

import com.roguesmp.dungeon.objective.IObjective;

import java.util.List;

public class Schemeta {
    private String schemId;
    private String schemName;
    private String schematic;
    //objective
    private List<IObjective> objectives;

    public Schemeta(String schemId, String schemName, String schematic, List<IObjective> objectives) {
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

    public List<IObjective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<IObjective> objectives) {
        this.objectives = objectives;
    }
}
