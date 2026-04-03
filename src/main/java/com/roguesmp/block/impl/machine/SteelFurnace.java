package com.roguesmp.block.impl.machine;

import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SteelFurnace extends ProcessingMachine {

    public SteelFurnace() {
        super(ItemRegistry.getInstance().getBaseItem("steel_furnace"), Material.FLINT_AND_STEEL);
    }

    @Override
    public int getEnergyPerSec() {
        return 0;
    }

    @Override
    public void registerRecipes(){
        ItemRegistry instance = ItemRegistry.getInstance();

        RecipeManager.register(new MachineRecipe("furnace_steel_ingot", getItem().getId(), 120)
                .addInput(List.of(ItemStack.of(Material.IRON_INGOT, 1), ItemStack.of(Material.COAL, 16)))
                .addOutput(instance.getBaseItem("steel_ingot").generateItemStack(1))
        );

        RecipeManager.register(new MachineRecipe("furnace_steel_block", getItem().getId(), 120 * 9)
                .addInput(List.of(ItemStack.of(Material.IRON_BLOCK, 1), ItemStack.of(Material.COAL_BLOCK, 16)))
                .addOutput(instance.getBaseItem("steel_block").generateItemStack(1))
        );

        RecipeManager.register(new MachineRecipe("furnace_condensed_steel", getItem().getId(), 120 * 9)
                .addInput(List.of(instance.getBaseItem("steel_ingot").generateItemStack(1), ItemStack.of(Material.COAL_BLOCK, 16)))
                .addOutput(instance.getBaseItem("condensed_steel").generateItemStack(1))
        );
    }

}
