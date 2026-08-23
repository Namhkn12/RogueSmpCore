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

/**
 * A single crafting ingredient/result reference by id, plus how many are required/produced.
 * <p>
 * {@link #key()} is either a {@code com.roguesmp.item.BaseItem} id as-is (e.g. {@code "fire_sword"})
 * or a vanilla material prefixed with {@value #VANILLA_PREFIX} (e.g. {@code "minecraft:iron_ingot"}) -
 * see {@link #resolveKey(ItemStack)}/{@link #resolveItemStack(String, int)} for the two-way mapping
 * against a real {@link ItemStack}.
 */
public record CraftingIngredient(String key, int count) {

    public static final String VANILLA_PREFIX = "minecraft:";

    /** Canonical object form: {@code {"item": "fire_sword", "count": 2}}. */
    public static final Codec<CraftingIngredient> OBJECT_CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(CraftingIngredient::key),
            Codec.INT.optionalFieldOf("count", 1).forGetter(CraftingIngredient::count),
            CraftingIngredient::new
    );

    /** Accepts the canonical object form, or a bare string shorthand implying {@code count = 1}. */
    public static final Codec<CraftingIngredient> CODEC = Codec.withAlternative(
            OBJECT_CODEC,
            Codec.STRING.xmap(key -> new CraftingIngredient(key, 1), CraftingIngredient::key)
    );

    public boolean isVanilla() {
        return key.startsWith(VANILLA_PREFIX);
    }

    /** Resolves this ingredient into a representative {@link ItemStack} (e.g. for GUI display). */
    public @Nullable ItemStack toItemStack() {
        return resolveItemStack(key, count);
    }

    /**
     * The canonical crafting-key for a real {@link ItemStack}: its {@code BaseItem} id if it's a
     * custom item, otherwise its material prefixed with {@value #VANILLA_PREFIX}. Returns
     * {@code null} for an empty/air stack.
     */
    public static @Nullable String resolveKey(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        if (stack.hasItemMeta()) {
            String id = stack.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
            if (id != null) return id;
        }

        return VANILLA_PREFIX + stack.getType().getKey().getKey();
    }

    /**
     * The inverse of {@link #resolveKey(ItemStack)} - builds a real {@link ItemStack} for a
     * crafting-key, or {@code null} if the key doesn't resolve to anything registered.
     */
    public static @Nullable ItemStack resolveItemStack(@Nullable String key, int amount) {
        if (key == null) return null;

        if (key.startsWith(VANILLA_PREFIX)) {
            Material material = Material.matchMaterial(key);
            return material == null ? null : ItemStack.of(material, amount);
        }

        BaseItem baseItem = Registries.ITEM.get(key);
        return baseItem == null ? null : baseItem.generateItemStack(amount);
    }

    /**
     * Sums stack sizes per resolved {@link #key()} across an array of slots (nulls/empty stacks
     * ignored) - the one place this "aggregate a runtime item array by crafting-key" logic lives,
     * shared by every recipe kind's own {@code matches(ItemStack[], int, int)} that needs a
     * position-independent count (shapeless/fusion/scrapping-style).
     */
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
