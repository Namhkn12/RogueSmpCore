package com.roguesmp.quest.board;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read from {@code quest_board.json}: how many quests a board shows, the quest level curve, and the optional
 * leaderboard-only top rank.
 */
public record QuestBoardSettings(int size, QuestLevelCurve level, @Nullable TopRankSettings topRank) {

    public static final int MAX_SIZE = 28;
    public static final QuestBoardSettings DEFAULT = new QuestBoardSettings(7, QuestLevelCurve.DEFAULT, null);

    public static final Codec<QuestBoardSettings> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("size", DEFAULT.size).forGetter(QuestBoardSettings::size),
            QuestLevelCurve.CODEC.optionalFieldOf("level", DEFAULT.level).forGetter(QuestBoardSettings::level),
            TopRankSettings.CODEC.optionalFieldOf("topRank", () -> null).forGetter(QuestBoardSettings::topRank),
            (size, level, topRank) -> new QuestBoardSettings(Math.clamp(size, 1, MAX_SIZE), level, topRank)
    );

    @Blocking
    public static QuestBoardSettings load(Path file) {
        if (!Files.exists(file)) return DEFAULT;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            DataResult<QuestBoardSettings> result = CODEC.decode(json, JsonOps.INSTANCE);
            if (result.isSuccess()) return result.result();
            RogueSmpCore.LOGGER.error("Invalid {}: {}", file.getFileName(), result.error());
        } catch (IOException | RuntimeException e) {
            RogueSmpCore.LOGGER.error("Failed to read {}", file.getFileName(), e);
        }
        return DEFAULT;
    }
}
