package com.roguesmp.action;

import com.roguesmp.action.impl.EntityZapperAction;
import com.roguesmp.action.impl.OpenGuiAction;
import com.roguesmp.action.impl.RunCommandAction;
import com.roguesmp.action.impl.TeleportAction;
import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class PlayerActions {

    public static final Codec<RunCommandAction> RUN_COMMAND = register(RunCommandAction.TYPE_KEY, RunCommandAction.CODEC);
    public static final Codec<OpenGuiAction> OPEN_GUI = register(OpenGuiAction.TYPE_KEY, OpenGuiAction.CODEC);
    public static final Codec<TeleportAction> TELEPORT = register(TeleportAction.TYPE_KEY, TeleportAction.CODEC);
    public static final Codec<EntityZapperAction> ENTITY_ZAPPER = register(EntityZapperAction.TYPE_KEY, EntityZapperAction.CODEC);

    public static void loadClass() {

    }

    private static <T extends PlayerAction> Codec<T> register(String key, Codec<T> codec) {
        return Registries.PLAYER_ACTION_CODEC.register(key, codec);
    }
}
