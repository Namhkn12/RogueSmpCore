package com.roguesmp.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Resource pack provided by <a href="https://github.com/AmberWat/NegativeSpaceFont">NegativeSpaceFont</a>
 */
public final class Space {

    private static SpaceEncoder encoder =
            new SpaceEncoder(
                    Key.key("default")
            );

    private Space() {
    }

    /**
     * Configures the font used by NegativeSpaceFont.
     */
    public static void configure(Key font) {
        encoder = new SpaceEncoder(font);
    }

    /**
     * Adds an integer pixel offset.
     */
    public static Component px(int pixels) {
        return encoder.encode(pixels);
    }

    /**
     * Starts a new font rendering layer.
     */
    public static Component newLayer() {
        return encoder.newLayer();
    }
}
