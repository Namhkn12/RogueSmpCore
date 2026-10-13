package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.registry.Registries;

/**
 * Every {@link DungeonFeature} type. Each constant registers itself into {@link Registries#DUNGEON_FEATURE_CODEC} as
 * it's initialized
 */
public class DungeonFeatures {

    public static final Codec<TriggersFeature> TRIGGERS = register(TriggersFeature.TYPE_KEY, TriggersFeature.CODEC);
    public static final Codec<QuestsFeature> QUESTS = register(QuestsFeature.TYPE_KEY, QuestsFeature.CODEC);
    public static final Codec<ExpiryFeature> EXPIRY = register(ExpiryFeature.TYPE_KEY, ExpiryFeature.CODEC);
    public static final Codec<MembershipFeature> MEMBERSHIP = register(MembershipFeature.TYPE_KEY, MembershipFeature.CODEC);
    public static final Codec<ScoreboardFeature> SCOREBOARD = register(ScoreboardFeature.TYPE_KEY, ScoreboardFeature.CODEC);

    public static void loadClass() {

    }

    private static <T extends DungeonFeature<?>> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_FEATURE_CODEC.register(typeName, codec);
    }
}
