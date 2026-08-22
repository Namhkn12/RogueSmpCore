package com.roguesmp.loot.event;

import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.context.LootContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired once per picked {@link LootEntry}, right after that entry's own JSON
 * {@code functions} ran on the {@link ItemStack}s it produced — the programmatic, per-entry
 * counterpart to {@link com.roguesmp.loot.event.LootRollCompleteEvent} (which only sees the
 * whole table's flattened result, with no way to tell which entry produced what).
 *
 * <pre>{@code
 * @EventHandler
 * public void onEntryResult(LootEntryResultEvent e) {
 *     if (!(e.getEntry() instanceof ItemEntry item)) return;
 *     if (!"legendary_sword".equals(item.getItemId())) return;
 *     for (ItemStack stack : e.getItems()) {
 *         SmpItemUtils.getSmpItem(stack).ifPresent(sword -> sword.addLore("Blessed by RNG"));
 *     }
 * }
 * }</pre>
 */
public class LootEntryResultEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull LootEntry entry;
    private final @NotNull LootContext context;
    private final @NotNull List<ItemStack> items;

    public LootEntryResultEvent(@NotNull LootEntry entry, @NotNull LootContext context, @NotNull List<ItemStack> items) {
        this.entry = entry;
        this.context = context;
        this.items = items;
    }

    /** The entry that was picked — inspect its weight/conditions/functions, or {@code instanceof} to a concrete kind for its own fields. */
    public @NotNull LootEntry getEntry() {
        return entry;
    }

    public @NotNull LootContext getContext() {
        return context;
    }

    /** Mutable — add/remove/replace freely, this is what gets folded into the table's results. */
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
