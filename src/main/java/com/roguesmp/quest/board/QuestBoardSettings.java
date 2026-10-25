package com.roguesmp.quest.board;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import org.jetbrains.annotations.Blocking;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read from {@code quest_board.json}: how many quests a board shows, and the quest level curve.
 */
public record QuestBoardSettings(int size, QuestLevelCurve level) {

    public static final int MAX_SIZE = 28;
    public static final QuestBoardSettings DEFAULT = new QuestBoardSettings(7, QuestLevelCurve.DEFAULT);

    public static final Codec<QuestBoardSettings> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("size", DEFAULT.size).forGetter(QuestBoardSettings::size),
            QuestLevelCurve.CODEC.optionalFieldOf("level", DEFAULT.level).forGetter(QuestBoardSettings::level),
            (size, level) -> new QuestBoardSettings(Math.clamp(size, 1, MAX_SIZE), level)
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
