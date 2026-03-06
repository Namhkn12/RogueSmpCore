package com.roguesmp.block.impl.machine;

import com.roguesmp.block.IEnergyStorage;
import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ElectricSteelFurnace extends ProcessingMachine implements IEnergyStorage {

    private int energy = 0;
    private final int MAX_ENERGY = 5000;
    private final int ENERGY_PER_SEC = 20;

    public ElectricSteelFurnace() {
        super(ItemRegistry.getInstance().getBaseItem("electric_steel_furnace"), Material.FLINT_AND_STEEL);

        ((MachineGui) gui).setEnergy(getEnergy(), getMaxEnergy());
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(int energy) {
        this.energy = energy;
        ((MachineGui) gui).setEnergy(getEnergy(), getMaxEnergy());
    }

    @Override
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public int getEnergyPerSec() {
        return ENERGY_PER_SEC;
    }

    @Override
    public void registerRecipes() {
        ItemRegistry instance = ItemRegistry.getInstance();

        RecipeManager.register(new MachineRecipe("steel_ingot", getItem().getId(), 30)
                .addInput(List.of(ItemStack.of(Material.IRON_INGOT, 1), ItemStack.of(Material.COAL, 16)))
                .addOutput(instance.getBaseItem("steel_ingot").generateItemStack(1))
        );

        RecipeManager.register(new MachineRecipe("steel_block", getItem().getId(), 30 * 9)
                .addInput(List.of(ItemStack.of(Material.IRON_BLOCK, 1), ItemStack.of(Material.COAL_BLOCK, 16)))
                .addOutput(instance.getBaseItem("steel_block").generateItemStack(1))
        );

        RecipeManager.register(new MachineRecipe("condensed_steel", getItem().getId(), 30 * 9)
                .addInput(List.of(instance.getBaseItem("steel_ingot").generateItemStack(1), ItemStack.of(Material.COAL_BLOCK, 16)))
                .addOutput(instance.getBaseItem("condensed_steel").generateItemStack(1))
        );
    }

}
