package com.roguesmp.text;

import net.kyori.adventure.text.Component;

import java.util.List;

public final class Layout {

    private final List<Entry> entries;

    Layout(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * @return A new component from the layout
     */
    public Component component() {
        Component result = Component.empty();

        for (Entry entry : entries) {
            if (entry.offset() != 0) {
                result = result.append(Space.px(entry.offset()));
            }
            result = result.append(entry.component());
        }

        return result;
    }

    public List<Entry> entries() {
        return entries;
    }

    public record Entry(int offset, Component component) { }

    public static LayoutBuilder builder() {
        return new LayoutBuilder();
    }
}
