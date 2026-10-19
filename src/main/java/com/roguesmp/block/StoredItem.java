package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.ItemRef;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record StoredItem(String item, int amount) {

    public static final Codec<StoredItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(StoredItem::item),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(StoredItem::amount),
            StoredItem::new
    );

    public static @Nullable StoredItem of(@Nullable ItemStack stack) {
        String id = ItemRef.idOf(stack);
        return id == null ? null : new StoredItem(id, stack.getAmount());
    }

    public StoredItem withAmount(int newAmount) {
        return new StoredItem(item, newAmount);
    }

    /**
     * Resolves this into an ItemStack with its stored (possibly larger-than-real-stack) amount - a
     * caller must make sure to clamp first (see {@link #maxStackSize}).
     */
    public @Nullable ItemStack toItemStack() {
        return ItemRef.toItemStack(item, amount);
    }

    public Component displayName() {
        return ItemRef.displayName(item);
    }

    public int maxStackSize() {
        return ItemRef.maxStackSize(item);
    }
}
