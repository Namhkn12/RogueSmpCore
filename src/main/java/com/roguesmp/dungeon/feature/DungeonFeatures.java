package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.registry.Registries;

public class DungeonFeatures {

    public static final Codec<TriggersFeature> TRIGGERS = register(TriggersFeature.TYPE_KEY, TriggersFeature.CODEC);
    public static final Codec<PartyFeature> PARTY = register(PartyFeature.TYPE_KEY, PartyFeature.CODEC);
    public static final Codec<QuestsFeature> QUESTS = register(QuestsFeature.TYPE_KEY, QuestsFeature.CODEC);
    public static final Codec<ExpiryFeature> EXPIRY = register(ExpiryFeature.TYPE_KEY, ExpiryFeature.CODEC);
    public static final Codec<MembershipFeature> MEMBERSHIP = register(MembershipFeature.TYPE_KEY, MembershipFeature.CODEC);
    public static final Codec<ReviveFeature> REVIVE = register(ReviveFeature.TYPE_KEY, ReviveFeature.CODEC);
    public static final Codec<RoomsFeature> ROOMS = register(RoomsFeature.TYPE_KEY, RoomsFeature.CODEC);
    public static final Codec<RewardChestsFeature> REWARD_CHESTS = register(RewardChestsFeature.TYPE_KEY, RewardChestsFeature.CODEC);
    public static final Codec<TreasureFeature> TREASURE = register(TreasureFeature.TYPE_KEY, TreasureFeature.CODEC);
    public static final Codec<ScoreboardFeature> SCOREBOARD = register(ScoreboardFeature.TYPE_KEY, ScoreboardFeature.CODEC);
    public static final Codec<BlockPalettesFeature> BLOCK_PALETTES = register(BlockPalettesFeature.TYPE_KEY, BlockPalettesFeature.CODEC);

    public static void loadClass() {

    }

    private static <T extends DungeonFeature<?>> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_FEATURE_CODEC.register(typeName, codec);
    }
}
