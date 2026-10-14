package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.NoState;

public record MembershipFeature(boolean invites, boolean abandon) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "membership";

    public static final Codec<MembershipFeature> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("invites", true).forGetter(MembershipFeature::invites),
            Codec.BOOLEAN.optionalFieldOf("abandon", true).forGetter(MembershipFeature::abandon),
            MembershipFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }
}
