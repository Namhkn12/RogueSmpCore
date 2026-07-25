package com.roguesmp.registry.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.ObjectiveProgress;
import com.roguesmp.quest.QuestObjective;
import com.roguesmp.quest.objective.KillMobObjective;
import com.roguesmp.registry.Registries;

public class QuestObjectiveRegistry {

    public static void bootstrap() {
        register(KillMobObjective.TYPE_KEY, KillMobObjective.CODEC, KillMobObjective.Progress.CODEC);
    }

    private static <T extends QuestObjective, P extends ObjectiveProgress> void register(String typeName, Codec<T> objCodec, Codec<P> progressCodec) {
        Registries.QUEST_OBJECTIVE_CODEC.register(typeName, objCodec);
        Registries.OBJECTIVE_PROGRESS_CODEC.register(typeName, progressCodec);
    }
}
