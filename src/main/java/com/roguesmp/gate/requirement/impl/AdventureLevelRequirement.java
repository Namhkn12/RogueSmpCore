package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public class AdventureLevelRequirement implements Requirement {

    public static final String TYPE_KEY = "adventure_level";
    public static final Codec<AdventureLevelRequirement> CODEC = Codec.INT.fieldOf("min")
            .xmap(AdventureLevelRequirement::new, AdventureLevelRequirement::getMin).codec();

    private final int min;

    public AdventureLevelRequirement(int min) {
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
        PlayerQuestData data = QuestManager.getInstance().getPlayerQuestData(player.getUuid());
        return data != null && QuestBoardManager.getInstance().getLevel(data) >= min;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>Cấp phiêu lưu <white>" + min);
    }
}
