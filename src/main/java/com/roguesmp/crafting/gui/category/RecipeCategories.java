package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockData;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.recipe.ScrappingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.BlockPlaceComponent;
import com.roguesmp.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeCategories {

    private static final RecipeCategory<CraftingRecipe> CRAFTING = new CraftingCategory();

    private static final Map<String, RecipeCategory<?>> BY_TYPE = Map.of(
            ShapedCraftingRecipe.TYPE_KEY, CRAFTING,
            ShapelessCraftingRecipe.TYPE_KEY, CRAFTING,
            FusionRecipe.TYPE_KEY, new FusionCategory(),
            CokeOvenRecipe.TYPE_KEY, new CokeOvenCategory(),
            ScrappingRecipe.TYPE_KEY, new ScrappingCategory()
    );

    private static final Map<String, RecipeCategory<?>> FALLBACKS = new HashMap<>();

    private RecipeCategories() {}

    public static RecipeCategory<?> of(CraftingRecipe recipe) {
        RecipeCategory<?> category = BY_TYPE.get(recipe.getTypeId());
        if (category != null) return category;
        return FALLBACKS.computeIfAbsent(recipe.getTypeId(), type -> new GenericCategory(type, recipe.getTypeName()));
    }

    @SuppressWarnings("unchecked")
    public static void layout(CraftingRecipe recipe, RecipeLayout layout) {
        ((RecipeCategory<CraftingRecipe>) of(recipe)).layout(recipe, layout);
    }

    public static List<CraftingRecipe> recipesIn(RecipeCategory<?> category) {
        List<CraftingRecipe> recipes = new ArrayList<>();
        for (CraftingRecipe recipe : Registries.CRAFTING_RECIPE.getAll().values()) {
            if (of(recipe) == category) recipes.add(recipe);
        }
        recipes.sort(Comparator.comparing(CraftingRecipe::getId));
        return recipes;
    }

    public static List<String> catalystsOf(RecipeCategory<?> category) {
        List<String> catalysts = new ArrayList<>(category.vanillaCatalysts());
        if (category.catalystBlocks().isEmpty()) return catalysts;

        for (Map.Entry<String, BaseItem> entry : Registries.ITEM.getAll().entrySet()) {
            BlockPlaceComponent placer = entry.getValue().getComponent(ItemComponentKeys.BLOCK_PLACE);
            if (placer == null) continue;

            BlockData block = Registries.BLOCK.get(placer.getBlockId());
            if (block != null && category.catalystBlocks().contains(block.type())) catalysts.add(entry.getKey());
        }
        return catalysts;
    }
}
