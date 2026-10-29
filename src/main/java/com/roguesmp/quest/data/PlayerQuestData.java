package com.roguesmp.quest.data;

import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.board.PlayerBoard;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.codec.Codec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represent progress data for all player quest.
 */
public class PlayerQuestData extends QuestLog {

    public static final Codec<PlayerQuestData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(PlayerQuestData::getUuid),
            Codec.lenientUnboundedMap(QuestProgress.CODEC).optionalFieldOf("questProgress", new HashMap<>()).forGetter(PlayerQuestData::getQuestProgresses),
            PlayerBoard.CODEC.optionalFieldOf("board", PlayerBoard::new).forGetter(PlayerQuestData::getBoard),
            Codec.LONG.optionalFieldOf("exp", 0L).forGetter(PlayerQuestData::getExp),
            Codec.LONG.optionalFieldOf("expUpdatedAt", 0L).forGetter(PlayerQuestData::getExpUpdatedAt),
            Codec.listOf(Codec.INT).optionalFieldOf("claimedRewards", () -> null).forGetter(data -> data.claimedRewards == null ? null : List.copyOf(data.claimedRewards)),
            PlayerQuestData::new
    );

    private final UUID uuid;
    private final PlayerHolder holder;
    private final PlayerBoard board;
    private long exp;
    private long expUpdatedAt;
    private @Nullable Set<Integer> claimedRewards = new HashSet<>();

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses, PlayerBoard board, long exp, long expUpdatedAt, @Nullable List<Integer> claimedRewards) {
        this.uuid = uuid;
        this.holder = new PlayerHolder(uuid);
        this.progresses.putAll(progresses);
        this.board = board;
        this.exp = exp;
        this.expUpdatedAt = expUpdatedAt;
        this.claimedRewards = claimedRewards == null ? null : new HashSet<>(claimedRewards);
        this.dirty = false;
    }

    public PlayerQuestData(PlayerQuestData toClone) {
        this.uuid = toClone.getUuid();
        this.holder = toClone.holder;
        this.progresses.putAll(toClone.getQuestProgresses());
        this.board = new PlayerBoard(toClone.board);
        this.exp = toClone.exp;
        this.expUpdatedAt = toClone.expUpdatedAt;
        this.claimedRewards = toClone.claimedRewards == null ? null : new HashSet<>(toClone.claimedRewards);
    }

    /**
     * Total quest exp earned; the level is derived from it through {@link com.roguesmp.quest.board.QuestLevelCurve}.
     */
    public long getExp() {
        return exp;
    }

    /**
     * Timestamp of when the current exp was reached
     */
    public long getExpUpdatedAt() {
        return expUpdatedAt;
    }

    public void addExp(long amount) {
        if (amount <= 0) return;
        exp += amount;
        expUpdatedAt = System.currentTimeMillis();
        dirty = true;
    }

    public void setExp(long exp) {
        this.exp = Math.max(0, exp);
        expUpdatedAt = System.currentTimeMillis();
        dirty = true;
    }

    /**
     * Null for saves made before level rewards became claimable, whose earlier rewards were already mailed out.
     */
    public @Nullable Set<Integer> getClaimedRewards() {
        return claimedRewards;
    }

    public void setClaimedRewards(Set<Integer> claimedRewards) {
        this.claimedRewards = new HashSet<>(claimedRewards);
        dirty = true;
    }

    public void markRewardClaimed(int level) {
        if (claimedRewards == null) claimedRewards = new HashSet<>();
        claimedRewards.add(level);
        dirty = true;
    }

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
        this.holder = new PlayerHolder(uuid);
        this.board = new PlayerBoard();
    }

    public PlayerBoard getBoard() {
        return board;
    }

    @Override
    public PlayerHolder getHolder() {
        return holder;
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
