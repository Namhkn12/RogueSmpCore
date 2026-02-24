package com.roguesmp.block.impl.machine;

import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.constant.Items;
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
        super(new BaseItem(
                "steel_furnace",
                Material.IRON_BLOCK,
                Map.of("name", new NameComponent("Steel Furnace"))
        ), Material.FLINT_AND_STEEL);

    }

    @Override
    public void registerRecipes(){
        RecipeManager.register(new MachineRecipe("steel_ingot", getItem().getId(), 120)
                .addInput(List.of(ItemStack.of(Material.IRON_INGOT, 1), ItemStack.of(Material.COAL, 16)))
                .addOutput(ItemRegistry.getInstance().getBaseItem("steel_ingot").generateItemStack(1))
        );

        RecipeManager.register(new MachineRecipe("steel_block", getItem().getId(), 120 * 9)
                .addInput(List.of(ItemStack.of(Material.IRON_BLOCK, 1), ItemStack.of(Material.COAL_BLOCK, 16)))
                .addOutput(ItemRegistry.getInstance().getBaseItem("steel_block").generateItemStack(1))
        );
    }

}
