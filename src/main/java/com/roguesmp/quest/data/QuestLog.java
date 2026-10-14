package com.roguesmp.quest.data;

import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.holder.QuestHolder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class QuestLog {

    protected final Map<String, QuestProgress> progresses = new HashMap<>();
    protected boolean dirty = true;

    public abstract QuestHolder getHolder();

    public @Unmodifiable Map<String, QuestProgress> getQuestProgresses() {
        return Collections.unmodifiableMap(progresses);
    }

    public @Nullable QuestProgress getQuestProgress(String questId) {
        return progresses.get(questId);
    }

    public void setQuestProgress(String questId, QuestProgress progress) {
        progresses.put(questId, progress);
        this.dirty = true;
    }

    public @Nullable QuestProgress removeQuestProgress(String questId) {
        this.dirty = true;
        return progresses.remove(questId);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
