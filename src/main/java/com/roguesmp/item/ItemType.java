package com.roguesmp.item;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.SmpItemUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Used in ItemTypeComponent. An item can be many types at once based on tags.
 */
public record ItemType(String tagId, String displayText) {

    public static final Codec<ItemType> CODEC = Codec.composite(
            Codec.STRING.fieldOf("tag").forGetter(ItemType::tagId),
            Codec.STRING.fieldOf("display").forGetter(ItemType::displayText),
            ItemType::new
    );

    /**
     * Whether {@code stack} is one of our own items tagged under {@link #tagId()}. A stack with no
     * resolvable {@link BaseItem} (empty, or not ours) never matches.
     */
    public boolean matches(@Nullable ItemStack stack) {
        BaseItem baseItem = SmpItemUtils.getBaseItem(stack);
        return baseItem != null && Registries.ITEM.getHolder(baseItem.getId()).getTagIds().contains(tagId);
    }
}
