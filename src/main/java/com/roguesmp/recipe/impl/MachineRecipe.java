package com.roguesmp.recipe.impl;

import com.roguesmp.constant.RecipeType;
import com.roguesmp.recipe.BaseRecipe;

public class MachineRecipe extends BaseRecipe {

    private final String machineId;
    private final int baseProcessTime;

    public MachineRecipe(String id, String machineId, int baseProcessTime) {
        super(id, RecipeType.PROCESSING);
        this.machineId = machineId;
        this.baseProcessTime = baseProcessTime;
    }

    public int getBaseProcessTime() {return baseProcessTime;}
    public String getMachineId() {return machineId;}
}
