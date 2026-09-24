package com.roguesmp.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.Objects;

/**
 * @param texture resource pack texture as {@code namespace:path/to/image.png}, relative to {@code textures/}
 * @param ascent  bitmap provider ascent, the distance in pixels from the baseline to the top of the glyph
 * @param height  bitmap provider height, the pixel size the texture is scaled to
 * @param width   advance in pixels this glyph is expected to take, for layout math only - Minecraft derives
 *                the real advance from the texture, so it is not written into the generated font
 */
public record Glyph(
        Key font,
        int codePoint,
        String texture,
        int ascent,
        int height,
        int width
) {

    public Glyph {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(texture, "texture");

        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid code point: " + codePoint);
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }

        if (width < 0) {
            throw new IllegalArgumentException("width cannot be negative");
        }
    }

    /**
     * Creates a component containing this glyph with font already set.
     */
    public Component create() {
        return Component.text().content(String.valueOf(Character.toChars(codePoint))).font(font).build();
    }

    /**
     * Creates a component containing this glyph
     * repeated count times.
     */
    public Component repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }

        if (count == 0) return Component.empty();

        String glyph = new String(Character.toChars(codePoint));
        String value = glyph.repeat(count);

        return Component.text(value).font(font);
    }
}
