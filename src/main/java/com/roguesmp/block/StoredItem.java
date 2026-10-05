package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.item.component.impl.StackSizeComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record StoredItem(String item, int amount) {

    public static final Codec<StoredItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(StoredItem::item),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(StoredItem::amount),
            StoredItem::new
    );

    public static @Nullable StoredItem of(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        String customId = ItemStackUtils.getBaseId(stack);
        String id = customId != null ? customId : BlockRef.vanillaId(stack.getType());
        return new StoredItem(id, stack.getAmount());
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

    public Component displayName() {
        if (BlockRef.isVanilla(item)) {
            Material material = BlockRef.vanillaMaterial(item);
            return material == null ? Component.text(item) : Component.translatable(material.translationKey());
        }

        BaseItem baseItem = Registries.ITEM.get(item);
        NameComponent name = baseItem == null ? null : baseItem.getComponent(ItemComponentKeys.ITEM_NAME);
        return name == null ? Component.text(item) : Utils.fromString(name.value());
    }

    public int maxStackSize() {
        if (BlockRef.isVanilla(item)) {
            Material material = BlockRef.vanillaMaterial(item);
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
        if (BlockRef.isVanilla(item)) {
            Material material = BlockRef.vanillaMaterial(item);
            return material == null ? null : ItemStack.of(material);
        }

        BaseItem baseItem = Registries.ITEM.get(item);
        return baseItem == null ? null : baseItem.generateItemStack(1);
    }
}
