package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.ItemType;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.registry.Holder;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * For adding item type lore based on tags. Not serializable.
 */
public class ItemTypeComponent implements ItemComponent {

    private final List<Holder<ItemType>> types;

    public ItemTypeComponent(List<Holder<ItemType>> types) {
        this.types = types;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        Component text = Component.text("Phân loại: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
        int providedLore = 0;
        for (Holder<ItemType> itemTypeHolder : types) {
            if (!itemTypeHolder.isBound()) continue;
            String displayText = itemTypeHolder.value().displayText();
            if (displayText == null || displayText.isEmpty()) continue;
            if (providedLore > 0) text = text.append(Component.text(", ", NamedTextColor.BLUE));
            text = text.append(Utils.fromString(displayText));
            providedLore++;
        }
        if (providedLore > 0) {
            context.builder().putLines(0, Collections.singletonList(text));
        }
    }
}
