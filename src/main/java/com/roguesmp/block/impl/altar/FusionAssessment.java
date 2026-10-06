package com.roguesmp.block.impl.altar;

import com.roguesmp.crafting.recipe.FusionRecipe;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record FusionAssessment(FusionIssue issue, @Nullable FusionRecipe recipe, List<AltarSideBlock> sides, ItemStack[] items) {

    public static FusionAssessment failed(FusionIssue issue) {
        return new FusionAssessment(issue, null, List.of(), new ItemStack[0]);
    }

    public boolean isReady() {
        return issue == FusionIssue.NONE;
    }

    public @Nullable ItemStack result() {
        return recipe == null ? null : recipe.getResultStack();
    }
}
