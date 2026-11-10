package com.roguesmp.event;

import com.roguesmp.block.StoredItem;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired when a player finishes a cycle on a hand-operated machine (current: hand crusher, hand sieve).
 */
public class HandMachineProcessEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String blockId;
    private final List<StoredItem> outputs;

    public HandMachineProcessEvent(Player player, String blockId, List<StoredItem> outputs) {
        super(player);
        this.blockId = blockId;
        this.outputs = outputs;
    }

    public String getBlockId() {
        return blockId;
    }

    public List<StoredItem> getOutputs() {
        return outputs;
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
