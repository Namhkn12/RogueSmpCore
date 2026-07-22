package com.roguesmp.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;

public class DailyResetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final LocalDate resetDate;
    private final ZoneId zoneId;

    public DailyResetEvent(LocalDate resetDate, ZoneId zoneId) {
        this.resetDate = resetDate;
        this.zoneId = zoneId;
    }

    public LocalDate getResetDate() {
        return resetDate;
    }

    public ZoneId getZoneId() {
        return zoneId;
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
