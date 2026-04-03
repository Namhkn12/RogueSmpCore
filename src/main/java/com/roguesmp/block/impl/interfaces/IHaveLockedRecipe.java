package com.roguesmp.block.impl.interfaces;

import com.roguesmp.recipe.BaseRecipe;

public interface IHaveLockedRecipe {
    BaseRecipe getLockedRecipe();
    void setLockedRecipe(BaseRecipe lockedRecipe);
}
