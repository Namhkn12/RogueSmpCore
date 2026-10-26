package com.roguesmp.teleport.effect;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;

public record NoTeleportEffect() implements TeleportEffect {

    public static final String TYPE_KEY = "none";
    public static final NoTeleportEffect INSTANCE = new NoTeleportEffect();

    public static final Codec<NoTeleportEffect> CODEC = MapCodec.unit(() -> INSTANCE).codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
