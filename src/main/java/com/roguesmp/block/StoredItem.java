package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.StackSizeComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.ItemStackUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record StoredItem(String item, int amount) {

    private static final String VANILLA_PREFIX = "minecraft:";

    public static final Codec<StoredItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(StoredItem::item),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(StoredItem::amount),
            StoredItem::new
    );

    public static @Nullable StoredItem of(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        String customId = ItemStackUtils.getBaseId(stack);
        String id = customId != null ? customId : vanillaId(stack.getType());
        return new StoredItem(id, stack.getAmount());
    }

    public static boolean isVanilla(String id) {
        return id.startsWith(VANILLA_PREFIX);
    }

    public static @Nullable Material vanillaMaterial(String id) {
        return isVanilla(id) ? Material.matchMaterial(id.substring(VANILLA_PREFIX.length())) : null;
    }

    public static String vanillaId(Material material) {
        return VANILLA_PREFIX + material.getKey().getKey();
    }

    public StoredItem withAmount(int newAmount) {
        return new StoredItem(item, newAmount);
    }

    /**
     * Resolves this into an ItemStack with its stored (possibly larger-than-real-stack) amount - a
     * caller must make sure to clamp first (see {@link #maxStackSize}).
     */
    public @Nullable ItemStack toItemStack() {
        ItemStack stack = resolve();
        if (stack == null) return null;

        stack.setAmount(amount);
        return stack;
    }

    public int maxStackSize() {
        if (isVanilla(item)) {
            Material material = vanillaMaterial(item);
            if (material == null) return 1;
            Integer stackSize = material.getDefaultData(DataComponentTypes.MAX_STACK_SIZE);
            return stackSize == null ? 1 : stackSize;
        }
        BaseItem baseItem = Registries.ITEM.get(item);
        if (baseItem == null) return 1;
        StackSizeComponent stackSizeComponent = baseItem.getComponent(ItemComponentKeys.STACK_SIZE);
        if (stackSizeComponent == null) {
            Integer stackSize = baseItem.getBase().getDefaultData(DataComponentTypes.MAX_STACK_SIZE);
            return stackSize == null ? 1 : stackSize;
        } else return stackSizeComponent.size();

    }

    private @Nullable ItemStack resolve() {
        if (isVanilla(item)) {
            Material material = vanillaMaterial(item);
            return material == null ? null : ItemStack.of(material);
        }

        BaseItem baseItem = Registries.ITEM.get(item);
        return baseItem == null ? null : baseItem.generateItemStack(1);
    }
}
