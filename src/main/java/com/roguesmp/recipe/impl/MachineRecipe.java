package com.roguesmp.recipe.impl;

import com.roguesmp.constant.RecipeType;
import com.roguesmp.recipe.BaseRecipe;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

public class MachineRecipe extends BaseRecipe {

    private final String machineId;
    private final int baseProcessTime;
    private int energyPerSec;

    public MachineRecipe(String id, String machineId, int baseProcessTime) {
        super(id, RecipeType.PROCESSING);
        this.machineId = machineId;
        this.baseProcessTime = baseProcessTime;
    }

    public MachineRecipe(String id, String machineId, @Nonnegative int baseProcessTime, @Nonnegative int energyPerSec){
        this(id, machineId, baseProcessTime);
        this.energyPerSec = energyPerSec;
    }

    public int getBaseProcessTime() {return baseProcessTime;}
    public String getMachineId() {return machineId;}
    public int getEnergyPerSec() {return energyPerSec;}
}
