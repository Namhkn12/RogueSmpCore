package com.roguesmp.server;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Map;

/**
 * Swaps every vanilla tool/armor crafting recipe whose result id matches one of our own item ids
 * (e.g. a "diamond_pickaxe" entry in {@code items/}) to instead craft that custom item, keeping the
 * vanilla recipe's shape and ingredients untouched. An id with no matching vanilla recipe, or one
 * that isn't a tool/armor piece, is left alone.
 */
public class VanillaRecipeReplacer {

    private static final String[] TOOL_ARMOR_SUFFIXES = {
            "_pickaxe", "_axe", "_shovel", "_hoe", "_sword",
            "_helmet", "_chestplate", "_leggings", "_boots"
    };

    public static void replaceAll() {
        int replaced = 0;
        for (Map.Entry<String, BaseItem> entry : Registries.ITEM.getAll().entrySet()) {
            if (replace(entry.getKey(), entry.getValue())) replaced++;
        }
        RogueSmpCore.LOGGER.info("Replaced {} vanilla tool/armor recipe(s) with custom items", replaced);
    }

    private static boolean replace(String id, BaseItem item) {
        if (!isToolOrArmorId(id)) return false;

        NamespacedKey key = NamespacedKey.minecraft(id);
        Recipe existing = Bukkit.getRecipe(key);
        if (!(existing instanceof ShapedRecipe vanilla)) return false;

        Bukkit.removeRecipe(key);

        ShapedRecipe replacement = new ShapedRecipe(key, item.generateItemStack(1));
        replacement.shape(vanilla.getShape());
        for (Map.Entry<Character, RecipeChoice> ingredient : vanilla.getChoiceMap().entrySet()) {
            if (ingredient.getValue() != null) replacement.setIngredient(ingredient.getKey(), ingredient.getValue());
        }
        replacement.setGroup(vanilla.getGroup());
        replacement.setCategory(vanilla.getCategory());

        return Bukkit.addRecipe(replacement);
    }

    private static boolean isToolOrArmorId(String id) {
        for (String suffix : TOOL_ARMOR_SUFFIXES) {
            if (id.endsWith(suffix)) return true;
        }
        return false;
    }
}
