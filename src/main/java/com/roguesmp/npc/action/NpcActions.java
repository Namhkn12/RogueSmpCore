package com.roguesmp.npc.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * Every {@link NpcAction} codec. Each constant registers itself into
 * {@link Registries#NPC_ACTION_CODEC} as it's initialized - call {@link #loadClass()} to force
 * that to happen.
 */
public class NpcActions {

    public static final Codec<RunCommandInteractAction> RUN_COMMAND = register(RunCommandInteractAction.TYPE_KEY, RunCommandInteractAction.CODEC);
    public static final Codec<OpenGuiInteractAction> OPEN_GUI = register(OpenGuiInteractAction.TYPE_KEY, OpenGuiInteractAction.CODEC);

    public static void loadClass() {

    }

    private static <T extends NpcAction> Codec<T> register(String key, Codec<T> codec) {
        return Registries.NPC_ACTION_CODEC.register(key, codec);
    }
}
