package com.roguesmp.recipe.manager;

import com.roguesmp.constant.Keys;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.EnergyRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.utils.RecipeUtils;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {
    private static final Map<String, BaseRecipe> recipeMap = new HashMap<>();
    private static final Map<String, List<BaseRecipe>> machineRecipes = new HashMap<>();

    public static void register(BaseRecipe recipe){
        recipeMap.put(recipe.getId(), recipe);
        if(recipe instanceof MachineRecipe mr){
            machineRecipes.computeIfAbsent(mr.getMachineId(), k -> new ArrayList<>()).add(mr);
        }
        if(recipe instanceof EnergyRecipe er){
            machineRecipes.computeIfAbsent(er.getMachineId(), k -> new ArrayList<>()).add(er);
        }
    }

    public static BaseRecipe findRecipe(String machineId, Inventory inv, int[] inputSlots){
        List<BaseRecipe> recipes = machineRecipes.get(machineId);
        if(recipes==null) return null;

        for(BaseRecipe recipe: recipes){
            if(RecipeUtils.matches(inv, inputSlots, recipe.getInputs())){
                return recipe;
            }
        }

        return null;
    }

    public static BaseRecipe findRecipeBasedOnMainOutput(String itemId, String machineId){
        List<BaseRecipe> recipes = machineRecipes.get(machineId);
        if(recipes==null || itemId==null) return null;

        for(BaseRecipe recipe: recipes) {
            if(recipe.getOutputs().isEmpty()) continue;

            ItemStack mainOutput = recipe.getOutputs().get(0);
            String mainOutputId = getSafeItemId(mainOutput);

            if(mainOutputId.equals(itemId)){
                return recipe;
            }
        }

        return null;
    }

    public static String getSafeItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return "AIR";

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(Keys.ITEM_ID, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        }

        // Nếu là item thường của game, trả về tên Material (vd: "IRON_INGOT")
        return item.getType().name();
    }

    public static BaseRecipe getRecipe(String recipeId) {
        if (recipeId == null) return null;
        return recipeMap.get(recipeId);
    }

    public static List<BaseRecipe> getMachineRecipesForMachine(String id){
        return machineRecipes.getOrDefault(id, new ArrayList<>());
    }
}
