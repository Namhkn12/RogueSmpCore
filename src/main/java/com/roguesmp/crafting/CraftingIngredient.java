package com.roguesmp.crafting;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.Registries;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record CraftingIngredient(String key, int count) {
    public static final String VANILLA_PREFIX = "minecraft:";

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
        return key.startsWith(VANILLA_PREFIX);
    }

    public @Nullable ItemStack toItemStack() {
        return resolveItemStack(key, count);
    }

    public static @Nullable String resolveKey(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        if (stack.hasItemMeta()) {
            String id = stack.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
            if (id != null) return id;
        }

        return VANILLA_PREFIX + stack.getType().getKey().getKey();
    }

    public static @Nullable ItemStack resolveItemStack(@Nullable String key, int amount) {
        if (key == null) return null;

        if (key.startsWith(VANILLA_PREFIX)) {
            Material material = Material.matchMaterial(key);
            return material == null ? null : ItemStack.of(material, amount);
        }

        BaseItem baseItem = Registries.ITEM.get(key);
        return baseItem == null ? null : baseItem.generateItemStack(amount);
    }

    public static @NotNull Map<String, Integer> aggregateCounts(@NotNull ItemStack @NotNull [] stacks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            String key = resolveKey(stack);
            if (key == null) continue;
            counts.merge(key, stack.getAmount(), Integer::sum);
        }
        return counts;
    }
}
