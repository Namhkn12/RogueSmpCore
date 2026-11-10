package com.roguesmp.quest.objective;

import com.roguesmp.codec.Codec;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.SmpPlayer;

import java.util.UUID;

public class IslandLevelObjective extends StateObjective {

    public static final String TYPE_KEY = "island_level";

    public static final Codec<IslandLevelObjective> CODEC = Codec.composite(
            Codec.INT.fieldOf("level").forGetter(IslandLevelObjective::getLevel),
            IslandLevelObjective::new
    );

    private final int level;

    public IslandLevelObjective(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    @Override
    protected boolean test(SmpPlayer smpPlayer) {
        UUID islandId = smpPlayer.getPlayerData().getIslandId();
        if (islandId == null) return false;
        IslandData island = IslandManager.getInstance().getIslandDataManager().getCachedData(islandId);
        return island != null && island.getLevel() >= level;
    }

    @Override
    protected String label() {
        return "Nâng đảo lên cấp " + level;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }
}
