package com.roguesmp.gui.interfaces;

import com.roguesmp.recipe.BaseRecipe;

public interface IHaveBlueprint {
    void setBlueprintItem(BaseRecipe lockedRecipe);
    void setupBlueprintSlot();
}
