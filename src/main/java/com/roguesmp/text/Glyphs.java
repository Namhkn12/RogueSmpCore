package com.roguesmp.text;

import com.roguesmp.constant.Keys;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.key.Key;

public final class Glyphs {

    private static final Key FONT = Keys.of("glyphs");

    private Glyphs() {
    }

    public static final Glyph SERVER_ICON = glyph("icon/server", '\uE000', 8, 7, 8);
    public static final Glyph COIN_ICON = glyph("icon/coin", '\uE001', 8, 7, 8);

    public static final Glyph FISHING_TRACK = glyph("fishing/track", '', 8, 7, 100);
    public static final Glyph FISHING_BAR_SEGMENT = glyph("fishing/bar_segment", '', 8, 7, 4);
    public static final Glyph FISHING_INDICATOR = glyph("fishing/indicator", '', 12, 9, 2);

    private static Glyph glyph(String name, int symbol, int height, int ascent, int width) {
        return glyph(name, symbol, Keys.GLOBAL_NAMESPACE + ":" + name + ".png", height, ascent, width);
    }

    private static Glyph glyph(String name, int symbol, String texture, int height, int ascent, int width) {
        Glyph glyph = new Glyph(FONT, symbol, texture, ascent, height, width);
        Registries.GLYPH.register(name, glyph);
        return glyph;
    }

    public static void bootstrap() {

    }
}
