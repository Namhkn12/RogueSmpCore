package com.roguesmp.billboard.line;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class BillboardLines {

    public static final Codec<TextLine> TEXT = register(TextLine.TYPE_KEY, TextLine.CODEC);
    public static final Codec<AnimatedLine> ANIMATED = register(AnimatedLine.TYPE_KEY, AnimatedLine.CODEC);

    public static void loadClass() {

    }

    private static <T extends BillboardLine> Codec<T> register(String key, Codec<T> codec) {
        return Registries.BILLBOARD_LINE_CODEC.register(key, codec);
    }
}
