package com.roguesmp.event;

import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.quest.QuestProgress;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an objective's progress actually changed, before the quest completes if this was its last objective.
 */
public class QuestProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final QuestHolder holder;
    private final QuestProgress progress;
    private final String objectiveId;

    public QuestProgressEvent(QuestHolder holder, QuestProgress progress, String objectiveId) {
        this.holder = holder;
        this.progress = progress;
        this.objectiveId = objectiveId;
    }

    public QuestHolder getHolder() {
        return holder;
    }

    public QuestProgress getProgress() {
        return progress;
    }

    public String getObjectiveId() {
        return objectiveId;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
