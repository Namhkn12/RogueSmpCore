package com.roguesmp.block.impl.generator.active;

import com.roguesmp.block.impl.type.ActiveGenerator;
import com.roguesmp.item.BaseItem;
import com.roguesmp.recipe.impl.EnergyRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.Burnable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class CoalGenerator extends ActiveGenerator {

    private final int GENERATION_RATE = 50;
    private final int TRANSFER_RATE = 100;
    private final int MAX_ENERGY = 5000;

    public CoalGenerator() {
        super(ItemRegistry.getInstance().getBaseItem("coal_generator"), Material.FLINT_AND_STEEL);
    }

    @Override
    public void registerRecipes() {
        ItemRegistry instance = ItemRegistry.getInstance();

        for(Map.Entry<Material, Integer> burnable: Burnable.getAllBurnableItems().entrySet()){
            if(burnable.getKey() != Material.LAVA_BUCKET){
                RecipeManager.register(new EnergyRecipe("coal_generator_" + burnable.getKey().name(), getItem().getId(), (int) Math.ceil(burnable.getValue().doubleValue() / 200))
                        .addInput(ItemStack.of(burnable.getKey(), 1))
                );
            }
        }
    }

    @Override
    public int getTransferRate() {
        return TRANSFER_RATE;
    }

    @Override
    public int getGenerationRate() {
        return GENERATION_RATE;
    }

    @Override
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }
}
