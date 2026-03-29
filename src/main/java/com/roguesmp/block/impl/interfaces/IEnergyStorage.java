package com.roguesmp.block.impl.interfaces;

public interface IEnergyStorage {
    int getEnergy();
    void setEnergy(int energy);
    int getMaxEnergy();

    default int receiveEnergy(int amount, boolean simulate){
        int energyReceived = Math.min(getMaxEnergy() - getEnergy(), amount);
        if(!simulate){
            setEnergy(getEnergy() + energyReceived);
        }

        return energyReceived;
    }

    default int extractEnergy(int amount, boolean simulate){
        int energyExtracted = Math.min(getEnergy(), amount);
        if(!simulate){
            setEnergy(getEnergy() - energyExtracted);
        }
        return energyExtracted;
    }
}
