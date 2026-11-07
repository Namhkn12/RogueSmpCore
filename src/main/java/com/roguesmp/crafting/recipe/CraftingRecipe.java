package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class CraftingRecipe extends Recipe {

    public static final MapCodec<Boolean> QUICK_CRAFT_CODEC = Codec.BOOLEAN.optionalFieldOf("quick_craft", true);

    private final boolean quickCraft;

    protected CraftingRecipe(@NotNull BaseProperties base, boolean quickCraft) {
        super(base, 1);
        this.quickCraft = quickCraft;
    }

    public abstract boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height);

    public abstract @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height);

    public boolean isQuickCraftable() {
        return quickCraft;
    }
}
