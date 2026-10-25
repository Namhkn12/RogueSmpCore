package com.roguesmp.player.inbox;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.reward.QuestReward;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Rewards waiting to be claimed. The rewards are copied in, so later changes to their source do not affect them.
 */
public record InboxEntry(UUID id, InboxSource source, List<QuestReward> rewards, long createdAt, long expiresAt) {

    public static final Duration CLAIM_WINDOW = Duration.ofDays(7);

    public static final Codec<InboxEntry> CODEC = Codec.composite(
            Codec.UUID.fieldOf("id").forGetter(InboxEntry::id),
            InboxSource.CODEC.fieldOf("source").forGetter(InboxEntry::source),
            Codec.lenientListOf(QuestReward.CODEC).fieldOf("rewards").forGetter(InboxEntry::rewards),
            Codec.LONG.fieldOf("createdAt").forGetter(InboxEntry::createdAt),
            Codec.LONG.fieldOf("expiresAt").forGetter(InboxEntry::expiresAt),
            InboxEntry::new
    );

    public InboxEntry {
        rewards = List.copyOf(rewards);
    }

    public static InboxEntry of(InboxSource source, List<QuestReward> rewards) {
        long now = System.currentTimeMillis();
        return new InboxEntry(UUID.randomUUID(), source, rewards, now, now + CLAIM_WINDOW.toMillis());
    }

    public boolean isExpired(long now) {
        return expiresAt <= now;
    }
}
