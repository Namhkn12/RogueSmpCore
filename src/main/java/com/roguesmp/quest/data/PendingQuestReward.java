package com.roguesmp.quest.data;

import com.roguesmp.quest.QuestProgress;
import com.roguesmp.codec.Codec;

import java.time.Duration;
import java.util.UUID;

/**
 * A completed quest's reward waiting in a player's inbox. {@code source} is the holder that completed the quest: the
 * player themselves, or a group.
 */
public record PendingQuestReward(UUID source, String questId, long expiresAt) {

    public static final Duration CLAIM_WINDOW = Duration.ofDays(7);

    public static final Codec<PendingQuestReward> CODEC = Codec.composite(
            Codec.UUID.fieldOf("source").forGetter(PendingQuestReward::source),
            Codec.STRING.fieldOf("questId").forGetter(PendingQuestReward::questId),
            Codec.LONG.fieldOf("expiresAt").forGetter(PendingQuestReward::expiresAt),
            PendingQuestReward::new
    );

    public static PendingQuestReward of(UUID source, QuestProgress progress) {
        return new PendingQuestReward(source, progress.getQuest().getId(), progress.getCompletedTimestamp() + CLAIM_WINDOW.toMillis());
    }

    public boolean matches(UUID source, String questId) {
        return this.source.equals(source) && this.questId.equals(questId);
    }
}
