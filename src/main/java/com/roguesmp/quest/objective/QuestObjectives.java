package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
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
    public static final Codec<SignalObjective> SIGNAL =
            register(SignalObjective.TYPE_KEY, SignalObjective.CODEC, SignalObjective.Progress.CODEC);
    public static final Codec<SubmitItemObjective> SUBMIT_ITEM =
            register(SubmitItemObjective.TYPE_KEY, SubmitItemObjective.CODEC, SubmitItemObjective.Progress.CODEC);
    public static final Codec<CraftItemObjective> CRAFT_ITEM =
            register(CraftItemObjective.TYPE_KEY, CraftItemObjective.CODEC, CraftItemObjective.Progress.CODEC);
    public static final Codec<HandProcessObjective> HAND_PROCESS =
            register(HandProcessObjective.TYPE_KEY, HandProcessObjective.CODEC, HandProcessObjective.Progress.CODEC);
    public static final Codec<ClassSelectedObjective> CLASS_SELECTED =
            register(ClassSelectedObjective.TYPE_KEY, ClassSelectedObjective.CODEC, StateObjective.Progress.codec(ClassSelectedObjective.TYPE_KEY));
    public static final Codec<HasIslandObjective> HAS_ISLAND =
            register(HasIslandObjective.TYPE_KEY, HasIslandObjective.CODEC, StateObjective.Progress.codec(HasIslandObjective.TYPE_KEY));
    public static final Codec<InHubObjective> IN_HUB =
            register(InHubObjective.TYPE_KEY, InHubObjective.CODEC, StateObjective.Progress.codec(InHubObjective.TYPE_KEY));
    public static final Codec<IslandLevelObjective> ISLAND_LEVEL =
            register(IslandLevelObjective.TYPE_KEY, IslandLevelObjective.CODEC, StateObjective.Progress.codec(IslandLevelObjective.TYPE_KEY));
    public static final Codec<DungeonClearObjective> DUNGEON_CLEAR =
            register(DungeonClearObjective.TYPE_KEY, DungeonClearObjective.CODEC, DungeonClearObjective.Progress.CODEC);

    public static final Codec<FishObjective> FISH =
            register(FishObjective.TYPE_KEY, FishObjective.CODEC, FishObjective.Progress.CODEC);

    public static void loadClass() {

    }

    private static <T extends QuestObjective, P extends ObjectiveProgress> Codec<T> register(
            String typeName, Codec<T> objCodec, Codec<P> progressCodec) {
        Registries.OBJECTIVE_PROGRESS_CODEC.register(typeName, progressCodec);
        return Registries.QUEST_OBJECTIVE_CODEC.register(typeName, objCodec);
    }
}
