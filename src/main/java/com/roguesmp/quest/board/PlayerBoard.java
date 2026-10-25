package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The quests offered to one player, and the cycle each board quest was last completed in.
 */
public class PlayerBoard {

    public static final Codec<PlayerBoard> CODEC = Codec.composite(
            Codec.listOf(BoardEntry.CODEC).optionalFieldOf("entries", List.of()).forGetter(PlayerBoard::getEntries),
            Codec.unboundedMap(Codec.LONG).optionalFieldOf("completedCycles", Map.of()).forGetter(PlayerBoard::getCompletedCycles),
            PlayerBoard::new
    );

    private final List<BoardEntry> entries = new ArrayList<>();
    private final Map<String, Long> completedCycles = new HashMap<>();

    public PlayerBoard(List<BoardEntry> entries, Map<String, Long> completedCycles) {
        this.entries.addAll(entries);
        this.completedCycles.putAll(completedCycles);
    }

    public PlayerBoard() {
    }

    public PlayerBoard(PlayerBoard toClone) {
        this(toClone.entries, toClone.completedCycles);
    }

    public @Unmodifiable List<BoardEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public @Unmodifiable Map<String, Long> getCompletedCycles() {
        return Collections.unmodifiableMap(completedCycles);
    }

    public @Nullable BoardEntry getEntry(String questId) {
        for (BoardEntry entry : entries) {
            if (entry.questId().equals(questId)) return entry;
        }
        return null;
    }

    public void addEntry(BoardEntry entry) {
        entries.add(entry);
    }

    public void removeEntry(BoardEntry entry) {
        entries.remove(entry);
    }

    public boolean isCompleted(String questId, long cycle) {
        Long completed = completedCycles.get(questId);
        return completed != null && completed == cycle;
    }

    public void markCompleted(String questId, long cycle) {
        completedCycles.put(questId, cycle);
    }

    public boolean removeCompletedIf(Predicate<Map.Entry<String, Long>> filter) {
        return completedCycles.entrySet().removeIf(filter);
    }
}
