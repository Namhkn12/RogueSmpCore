package com.roguesmp.item;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.utils.SmpItemUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Used in ItemTypeComponent. An item can be many types at once based on tags.
 * {@code tag} defaults to the type's own id.
 */
public record ItemType(String id, String tagId, String displayText, Holder<ItemTypeGroup> group) {

    public static final Codec<ItemType> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(ItemType::id),
            Codec.STRING.optionalFieldOf("tag").forGetter(type -> Optional.of(type.tagId())),
            Codec.STRING.fieldOf("display").forGetter(ItemType::displayText),
            Registry.referenceCodec(() -> Registries.ITEM_TYPE_GROUP).optionalFieldOf("group", () -> Registries.ITEM_TYPE_GROUP.getHolder("classification")).forGetter(ItemType::group),
            (id, tag, display, group) -> new ItemType(id, tag.orElse(id), display, group)
    );

    /**
     * Whether {@code stack} is one of our own items tagged under {@link #tagId()}. A stack with no
     * resolvable {@link BaseItem} (empty, or not ours) never matches.
     */
    public boolean matches(@Nullable ItemStack stack) {
        BaseItem baseItem = SmpItemUtils.getBaseItem(stack);
        return baseItem != null && matches(baseItem.getId());
    }

    public boolean matches(String itemId) {
        return Registries.ITEM.getHolder(itemId).getTagIds().contains(tagId);
    }
}
