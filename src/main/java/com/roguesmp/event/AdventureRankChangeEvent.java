package com.roguesmp.event;

import com.roguesmp.quest.board.AdventureRank;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AdventureRankChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final AdventureRank oldRank;
    private final AdventureRank newRank;

    public AdventureRankChangeEvent(Player player, AdventureRank oldRank, AdventureRank newRank) {
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public Player getPlayer() {
        return player;
    }

    public AdventureRank getOldRank() {
        return oldRank;
    }

    public AdventureRank getNewRank() {
        return newRank;
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
