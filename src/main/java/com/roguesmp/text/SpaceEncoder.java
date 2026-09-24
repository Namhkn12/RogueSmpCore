package com.roguesmp.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Resource pack provided by <a href="https://github.com/AmberWat/NegativeSpaceFont">NegativeSpaceFont</a>
 */
public final class SpaceEncoder {

    private static final int INTEGER_BASE = 0xD0000;

    private static final int MIN_INTEGER = -8192;
    private static final int MAX_INTEGER = 8192;

    private static final int NEW_LAYER = 0xC0000;

    private final Key font;

    public SpaceEncoder(Key font) {
        this.font = font;
    }

    public Component encode(int pixels) {
        if (pixels >= MIN_INTEGER && pixels <= MAX_INTEGER) {
            return glyph(INTEGER_BASE + pixels);
        }

        return encodeLarge(pixels);
    }

    public Component newLayer() {
        return glyph(NEW_LAYER);
    }

    private Component encodeLarge(int pixels) {
        Component result = Component.empty();

        int remaining = pixels;

        while (remaining != 0) {
            int step = Math.max(MIN_INTEGER, Math.min(MAX_INTEGER, remaining));

            result = result.append(glyph(INTEGER_BASE + step));

            remaining -= step;
        }

        return result;
    }

    private Component glyph(int codePoint) {
        return Component.text(String.valueOf(Character.toChars(codePoint))).font(font);
    }
}
