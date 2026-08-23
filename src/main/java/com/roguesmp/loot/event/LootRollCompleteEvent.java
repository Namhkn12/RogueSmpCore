package com.roguesmp.loot.event;

import com.roguesmp.loot.context.LootContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired once after a root loot table finishes rolling, carrying the final flattened item list —
 * the complement to {@link LootRollEvent} (which fires before rolling starts). Listeners can
 * freely add/remove/multiply {@link #getItems()} in place; whatever the list looks like when
 * this event finishes is exactly what {@link com.roguesmp.loot.service.ILootService#roll} returns.
 *
 * <p>Use this for effects that need to act on the <b>whole</b> result rather than a single
 * entry — a global "double drops" event, injecting a bonus item unconditionally, deduplicating
 * stacks — instead of needing every entry in every table to carry its own
 * {@link com.roguesmp.loot.function.LootFunction}.
 *
 * <pre>{@code
 * @EventHandler
 * public void onLootRollComplete(LootRollCompleteEvent e) {
 *     if (!doubleDropsActive) return;
 *     List<ItemStack> doubled = new ArrayList<>(e.getItems());
 *     e.getItems().addAll(doubled);
 * }
 * }</pre>
 *
 * <p>Not cancellable — to veto a roll entirely, cancel {@link LootRollEvent} before it starts
 * instead. Only fires once per root {@link com.roguesmp.loot.service.ILootService#roll} call,
 * same as {@link LootRollEvent} — nested {@code LOOT_TABLE} entries don't fire their own.
 */
public class LootRollCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String lootTableId;
    private final @NotNull LootContext context;
    private final @NotNull List<ItemStack> items;

    public LootRollCompleteEvent(@NotNull String lootTableId, @NotNull LootContext context, @NotNull List<ItemStack> items) {
        this.lootTableId = lootTableId;
        this.context = context;
        this.items = items;
    }

    /** ID of the root loot table that was rolled, e.g. {@code "dungeons/dungeon_a_reward"}. */
    public @NotNull String getLootTableId() {
        return lootTableId;
    }

    public @NotNull LootContext getContext() {
        return context;
    }

    /** Mutable — this exact list (or whatever it looks like after listeners run) is returned to the caller. */
    public @NotNull List<ItemStack> getItems() {
        return items;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
