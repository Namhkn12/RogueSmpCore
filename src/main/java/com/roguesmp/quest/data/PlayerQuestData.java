package com.roguesmp.quest.data;

import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.board.PlayerBoard;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.codec.Codec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
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
            PlayerQuestData::new
    );

    private final UUID uuid;
    private final PlayerHolder holder;
    private final PlayerBoard board;
    private long exp;

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses, PlayerBoard board, long exp) {
        this.uuid = uuid;
        this.holder = new PlayerHolder(uuid);
        this.progresses.putAll(progresses);
        this.board = board;
        this.exp = exp;
        this.dirty = false;
    }

    public PlayerQuestData(PlayerQuestData toClone) {
        this.uuid = toClone.getUuid();
        this.holder = toClone.holder;
        this.progresses.putAll(toClone.getQuestProgresses());
        this.board = new PlayerBoard(toClone.board);
        this.exp = toClone.exp;
    }

    /**
     * Total quest exp earned; the level is derived from it through {@link com.roguesmp.quest.board.QuestLevelCurve}.
     */
    public long getExp() {
        return exp;
    }

    public void addExp(long amount) {
        if (amount <= 0) return;
        exp += amount;
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
