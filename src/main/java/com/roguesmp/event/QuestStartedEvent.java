package com.roguesmp.event;

import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.quest.QuestProgress;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QuestStartedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final QuestHolder holder;
    private final QuestProgress progress;

    public QuestStartedEvent(QuestHolder holder, QuestProgress progress) {
        this.holder = holder;
        this.progress = progress;
    }

    public QuestHolder getHolder() {
        return holder;
    }

    public QuestProgress getProgress() {
        return progress;
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
