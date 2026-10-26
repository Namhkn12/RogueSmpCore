package com.roguesmp.teleport.effect;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeleportEffects {

    public static final Codec<NoTeleportEffect> NONE = register(NoTeleportEffect.TYPE_KEY, NoTeleportEffect.CODEC);
    public static final Codec<EndermanTeleportEffect> ENDERMAN = register(EndermanTeleportEffect.TYPE_KEY, EndermanTeleportEffect.CODEC);

    public static final Map<String, TeleportEffect> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put(NoTeleportEffect.TYPE_KEY, NoTeleportEffect.INSTANCE);
        PRESETS.put(EndermanTeleportEffect.TYPE_KEY, EndermanTeleportEffect.DEFAULT);
    }

    public static void loadClass() {

    }

    public static TeleportEffect next(TeleportEffect current) {
        List<String> keys = List.copyOf(PRESETS.keySet());
        int index = keys.indexOf(current.getTypeId());
        return PRESETS.get(keys.get((index + 1) % keys.size()));
    }

    private static <T extends TeleportEffect> Codec<T> register(String key, Codec<T> codec) {
        return Registries.TELEPORT_EFFECT_CODEC.register(key, codec);
    }
}
