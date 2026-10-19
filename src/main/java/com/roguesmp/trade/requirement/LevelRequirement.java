package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public class LevelRequirement implements TradeRequirement {

    public static final String TYPE_KEY = "level";
    public static final Codec<LevelRequirement> CODEC = Codec.composite(
            Codec.INT.fieldOf("min").forGetter(LevelRequirement::getMin),
            LevelRequirement::new
    );

    private final int min;

    public LevelRequirement(int min) {
        this.min = min;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public int getMin() {
        return min;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return player.getPlayerData().getLevel() >= min;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>Cấp độ <white>" + min);
    }
}
