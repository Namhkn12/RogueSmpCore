package com.roguesmp.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.UUID;

public class DungeonClearEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String dungeonId;
    private final Set<UUID> members;

    public DungeonClearEvent(String dungeonId, Set<UUID> members) {
        this.dungeonId = dungeonId;
        this.members = Set.copyOf(members);
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public @Unmodifiable Set<UUID> getMembers() {
        return members;
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
