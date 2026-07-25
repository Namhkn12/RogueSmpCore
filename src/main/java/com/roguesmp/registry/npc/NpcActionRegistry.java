package com.roguesmp.registry.npc;

import com.roguesmp.codec.Codec;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.npc.action.OpenGuiInteractAction;
import com.roguesmp.npc.action.RunCommandInteractAction;
import com.roguesmp.registry.Registries;

/**
 * Hold action type definition
 */
public class NpcActionRegistry {

    public static void bootstrap() {
        register(RunCommandInteractAction.TYPE_KEY, RunCommandInteractAction.CODEC);
        register(OpenGuiInteractAction.TYPE_KEY, OpenGuiInteractAction.CODEC);
    }

    private static <T extends NpcAction> void register(String key, Codec<T> codec) {
        Registries.NPC_ACTION_CODEC.register(key, codec);
    }


}
