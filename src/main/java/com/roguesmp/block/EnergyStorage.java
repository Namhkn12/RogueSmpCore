package com.roguesmp.block;

public interface EnergyStorage {
    int getEnergy();
    int getMaxEnergy();

    /**
     * @return amount actually accepted
     */
    int addEnergy(int amount);

    /**
     * @return amount actually removed
     */
    int removeEnergy(int amount);
}
