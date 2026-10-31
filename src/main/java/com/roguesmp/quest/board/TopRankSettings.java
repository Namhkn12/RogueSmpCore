package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;

public record TopRankSettings(int slots, int minLevel, AdventureRank rank) {

    public static final Codec<TopRankSettings> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("slots", 10).forGetter(TopRankSettings::slots),
            Codec.INT.optionalFieldOf("minLevel", 60).forGetter(TopRankSettings::minLevel),
            AdventureRank.CODEC.fieldOf("rank").forGetter(TopRankSettings::rank),
            (slots, minLevel, rank) -> new TopRankSettings(Math.max(1, slots), Math.max(1, minLevel), rank)
    );
}
