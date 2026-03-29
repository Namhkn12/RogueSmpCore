package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class VanillaCraftingRecipeRegistry {
    private static VanillaCraftingRecipeRegistry INSTANCE = null;
    private final List<Recipe> recipes = new ArrayList<>();

    private final RogueSmpCore plugin;

    public VanillaCraftingRecipeRegistry(RogueSmpCore plugin){
        this.plugin = plugin;

        setup();
    }

    private void setup(){
        steelFurnaceRecipe();
        steelBlockRecipe();
        electricSteelFurnaceRecipe();

        int numberOfCustomRecipeAdded = 0;
        for(Recipe recipe : recipes){
            boolean added = Bukkit.addRecipe(recipe);

            if(added) numberOfCustomRecipeAdded++;
        }

        plugin.getLogger().info("Loaded custom recipes (" + numberOfCustomRecipeAdded + " entries)");
    }

    private void steelFurnaceRecipe(){
        ItemStack steelFurnace = ItemRegistry.getInstance().getBaseItem("steel_furnace").generateItemStack(1);
        NamespacedKey recipeKey = new NamespacedKey(plugin, "steel_furnace");

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, steelFurnace);
        recipe.shape(
                "#M#",
                "#F#",
                "#B#"
        );

        recipe.setIngredient('#', Material.IRON_BLOCK);
        recipe.setIngredient('M', Material.MAGMA_BLOCK);
        recipe.setIngredient('F', Material.FLINT_AND_STEEL);
        recipe.setIngredient('B', Material.BLAST_FURNACE);

        recipes.add(recipe);
    }
    private void steelBlockRecipe(){
        ItemStack steelBlock = ItemRegistry.getInstance().getBaseItem("steel_block").generateItemStack(1);
        ItemStack steelIngot = ItemRegistry.getInstance().getBaseItem("steel_ingot").generateItemStack(1);
        NamespacedKey recipeKey = new NamespacedKey(plugin, "steel_block");

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, steelBlock);
        recipe.shape(
                "###",
                "###",
                "###"
        );

        recipe.setIngredient('#', new RecipeChoice.ExactChoice(steelIngot));

        recipes.add(recipe);
    }

    private void electricSteelFurnaceRecipe(){
        ItemStack electricSteelFurnace = ItemRegistry.getInstance().getBaseItem("electric_steel_furnace").generateItemStack(1);
        ItemStack steelBlock = ItemRegistry.getInstance().getBaseItem("steel_block").generateItemStack(1);
        ItemStack steelFurnace = ItemRegistry.getInstance().getBaseItem("steel_furnace").generateItemStack(1);
        NamespacedKey recipeKey = new NamespacedKey(plugin, "electric_steel_furnace");

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, electricSteelFurnace);
        recipe.shape(
            "#B#",
            "#F#",
            "#B#"
        );

        recipe.setIngredient('#', new RecipeChoice.ExactChoice(steelBlock));
        recipe.setIngredient('F', Material.FLINT_AND_STEEL);
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(steelFurnace));

        recipes.add(recipe);
    }

    public static void init(RogueSmpCore plugin) {INSTANCE = new VanillaCraftingRecipeRegistry(plugin);}
}
