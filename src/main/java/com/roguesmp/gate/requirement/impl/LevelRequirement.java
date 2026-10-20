package com.roguesmp.gate.requirement.impl;

import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.codec.Codec;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public class LevelRequirement implements Requirement {

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
        IslandData island = IslandManager.getInstance().getIslandDataManager().getCachedData(player.getPlayerData().getIslandId());
        return island != null && island.getLevel() >= min;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>Cấp độ đảo <white>" + min);
    }
}
