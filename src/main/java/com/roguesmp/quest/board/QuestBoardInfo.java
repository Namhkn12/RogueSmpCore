package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;

/**
 * Marks a quest as part of the quest board pool.
 */
public record QuestBoardInfo(ResetSchedule reset, int weight) {

    public static final Codec<QuestBoardInfo> CODEC = Codec.composite(
            ResetSchedule.CODEC.fieldOf("reset").forGetter(QuestBoardInfo::reset),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(QuestBoardInfo::weight),
            QuestBoardInfo::new
    );
}
