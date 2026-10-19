package com.roguesmp.crafting;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.ItemRef;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record CraftingIngredient(String key, int count) {

    public static final Codec<CraftingIngredient> OBJECT_CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(CraftingIngredient::key),
            Codec.INT.optionalFieldOf("count", 1).forGetter(CraftingIngredient::count),
            CraftingIngredient::new
    );

    public static final Codec<CraftingIngredient> CODEC = Codec.withAlternative(
            OBJECT_CODEC,
            Codec.STRING.xmap(key -> new CraftingIngredient(key, 1), CraftingIngredient::key)
    );

    public boolean isVanilla() {
        return ItemRef.isVanilla(key);
    }

    public @Nullable ItemStack toItemStack() {
        return ItemRef.toItemStack(key, count);
    }

    public static @NotNull Map<String, Integer> aggregateCounts(@NotNull ItemStack @NotNull [] stacks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            String key = ItemRef.idOf(stack);
            if (key == null) continue;
            counts.merge(key, stack.getAmount(), Integer::sum);
        }
        return counts;
    }
}
