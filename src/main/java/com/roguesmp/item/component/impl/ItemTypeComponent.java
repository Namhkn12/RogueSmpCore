package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.ItemType;
import com.roguesmp.item.ItemTypeGroup;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.registry.Holder;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * For adding item type lore based on tags. Not serializable.
 */
public class ItemTypeComponent implements ItemComponent {

    private static final Comparator<Holder<ItemTypeGroup>> GROUP_ORDER = Comparator
            .comparingInt((Holder<ItemTypeGroup> group) -> group.isBound() ? group.value().priority() : Integer.MAX_VALUE)
            .thenComparing(Holder::getId);

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
        Map<Holder<ItemTypeGroup>, List<String>> displaysByGroup = new TreeMap<>(GROUP_ORDER);
        types.stream()
                .filter(Holder::isBound)
                .map(Holder::value)
                .filter(type -> type.displayText() != null && !type.displayText().isEmpty())
                .sorted(Comparator.comparing(ItemType::id))
                .forEach(type -> displaysByGroup.computeIfAbsent(type.group(), group -> new ArrayList<>()).add(type.displayText()));

        List<Component> lines = new ArrayList<>();
        displaysByGroup.forEach((group, displays) -> {
            String label = group.isBound() ? group.value().display() : group.getId();
            Component line = Component.text(label + ": ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
            for (int i = 0; i < displays.size(); i++) {
                if (i > 0) line = line.append(Component.text(", ", NamedTextColor.BLUE));
                line = line.append(Utils.fromString(displays.get(i)));
            }
            lines.add(line);
        });
        if (!lines.isEmpty()) context.builder().putLines(0, lines);
    }
}
