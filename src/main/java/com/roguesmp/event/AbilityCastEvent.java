package com.roguesmp.event;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AbilityCastEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SmpPlayer smpPlayer;
    private final Ability ability;
    private boolean cancelled;

    public AbilityCastEvent(SmpPlayer smpPlayer, Ability ability) {
        this.smpPlayer = smpPlayer;
        this.ability = ability;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public SmpPlayer getSmpPlayer() {
        return smpPlayer;
    }

    public Ability getAbility() {
        return ability;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
