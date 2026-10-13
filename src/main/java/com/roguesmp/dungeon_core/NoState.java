package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;

/**
 * State of a feature that keeps nothing per run.
 */
public enum NoState {
    INSTANCE;

    public static final Codec<NoState> CODEC = Codec.unit(() -> INSTANCE);
}
