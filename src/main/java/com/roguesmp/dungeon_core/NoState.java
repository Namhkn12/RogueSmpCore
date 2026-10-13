package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;

public enum NoState {
    INSTANCE;

    public static final Codec<NoState> CODEC = Codec.unit(() -> INSTANCE);
}
