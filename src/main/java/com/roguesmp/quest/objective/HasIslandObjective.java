package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.player.SmpPlayer;

public class HasIslandObjective extends StateObjective {

    public static final String TYPE_KEY = "has_island";
    public static final HasIslandObjective INSTANCE = new HasIslandObjective();

    public static final Codec<HasIslandObjective> CODEC = MapCodec.unit(() -> INSTANCE).codec();

    @Override
    protected boolean test(SmpPlayer smpPlayer) {
        return smpPlayer.getPlayerData().getIslandId() != null;
    }

    @Override
    protected String label() {
        return "Tạo hoặc tham gia một hòn đảo";
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
