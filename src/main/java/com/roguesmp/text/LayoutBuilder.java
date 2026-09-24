package com.roguesmp.text;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class LayoutBuilder {

    private final List<Layout.Entry> entries = new ArrayList<>();

    LayoutBuilder() {
    }

    /**
     * Adds a component without moving first.
     */
    public LayoutBuilder add(Component component) {
        entries.add(new Layout.Entry(0, component));
        return this;
    }

    /**
     * Adds a glyph without moving first.
     */
    public LayoutBuilder add(Glyph glyph) {
        return add(glyph.create());
    }

    /**
     * Adds a glyph repeated multiple times as a single component.
     */
    public LayoutBuilder add(Glyph glyph, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }

        if (count == 0) return this;

        return add(glyph.repeat(count));
    }

    /**
     * Adds a component repeated multiple times.
     */
    public LayoutBuilder add(Component component, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }

        if (count == 0) return this;

        for (int i = 0; i < count; i++) {
            entries.add(new Layout.Entry(0, component));
        }

        return this;
    }

    /**
     * Moves horizontally.
     */
    public LayoutBuilder move(int pixels) {
        entries.add(new Layout.Entry(pixels, Component.empty()));

        return this;
    }

    /**
     * Moves horizontally and then adds a component.
     */
    public LayoutBuilder moveThen(int pixels, Component component) {
        entries.add(new Layout.Entry(pixels, component));

        return this;
    }

    /**
     * Moves horizontally and then adds a glyph.
     */
    public LayoutBuilder moveThen(int pixels, Glyph glyph) {
        return moveThen(pixels, glyph.create());
    }

    /**
     * Moves horizontally and then adds repeated glyphs.
     */
    public LayoutBuilder moveThen(int pixels, Glyph glyph, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }

        if (count == 0) return move(pixels);

        return moveThen(pixels, glyph.repeat(count));
    }

    public LayoutBuilder offset(int before, Component component, int after) {
        entries.add(new Layout.Entry(before, Component.empty()));
        entries.add(new Layout.Entry(0, component));
        entries.add(new Layout.Entry(after, Component.empty()));

        return this;
    }

    /**
     * Adds a glyph with a temporary horizontal offset.
     */
    public LayoutBuilder offset(int before, Glyph glyph, int after) {
        return offset(before, glyph.create(), after);
    }

    /**
     * Adds repeated glyphs with a temporary horizontal offset.
     */
    public LayoutBuilder offset(int before, Glyph glyph, int count, int after) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }

        if (count == 0) return move(before + after);

        return offset(before, glyph.repeat(count), after);
    }

    public Layout build() {
        return new Layout(entries);
    }

    /**
     * @return A new component from this builder.
     */
    public Component component() {
        return build().component();
    }
}
