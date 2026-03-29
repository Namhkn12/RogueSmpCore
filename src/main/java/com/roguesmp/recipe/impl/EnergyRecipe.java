package com.roguesmp.recipe.impl;

import com.roguesmp.constant.RecipeType;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.IProcessableRecipe;

import javax.annotation.Nonnegative;

public class EnergyRecipe extends BaseRecipe implements IProcessableRecipe {
    private final String machineId;
    private final int baseProcessTime;
    private int energyPerSec;

    public EnergyRecipe(String id, String machineId, int baseProcessTime) {
        super(id, RecipeType.ENERGY);
        this.machineId = machineId;
        this.baseProcessTime = baseProcessTime;
    }

    public EnergyRecipe(String id, String machineId, @Nonnegative int baseProcessTime, @Nonnegative int energyPerSec){
        this(id, machineId, baseProcessTime);
        this.energyPerSec = energyPerSec;
    }

    @Override
    public int getBaseProcessTime() {return baseProcessTime;}
    public String getMachineId() {return machineId;}
    public int getEnergyPerSec() {return energyPerSec;}
}
