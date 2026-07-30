package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.objective.KillMobObjective;
import com.roguesmp.registry.Registries;

/**
 * Every {@link QuestObjective} codec (and its matching {@link ObjectiveProgress} codec). Each
 * constant registers itself into {@link Registries#QUEST_OBJECTIVE_CODEC}/
 * {@link Registries#OBJECTIVE_PROGRESS_CODEC} as it's initialized - call {@link #loadClass()} to
 * force that to happen.
 */
public class QuestObjectives {

    public static final Codec<KillMobObjective> KILL_MOB =
            register(KillMobObjective.TYPE_KEY, KillMobObjective.CODEC, KillMobObjective.Progress.CODEC);

    public static void loadClass() {

    }

    private static <T extends QuestObjective, P extends ObjectiveProgress> Codec<T> register(
            String typeName, Codec<T> objCodec, Codec<P> progressCodec) {
        Registries.OBJECTIVE_PROGRESS_CODEC.register(typeName, progressCodec);
        return Registries.QUEST_OBJECTIVE_CODEC.register(typeName, objCodec);
    }
}
