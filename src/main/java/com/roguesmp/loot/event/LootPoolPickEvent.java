package com.roguesmp.loot.event;

import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.LootPool;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Fired once per weighted-pick attempt inside a {@link LootPool} (i.e. once per roll iteration,
 * right before an entry is chosen) — the programmatic way to influence which entry gets picked.
 * Every entry in the pool is exposed as a {@link Candidate} carrying the real {@link LootEntry}
 * and its currently-computed {@code weight}/{@code eligible} (after that entry's own JSON
 * {@code conditions} already ran) — read the entry's actual data (weight, and kind-specific
 * fields via {@code instanceof} on a concrete kind like {@code ItemEntry}) and override the
 * outcome directly in code, including force-including an entry whose JSON conditions failed, or
 * force-excluding one that passed.
 *
 * <pre>{@code
 * @EventHandler
 * public void onPoolPick(LootPoolPickEvent e) {
 *     SmpPlayer player = e.getContext().getPlayer();
 *     if (player == null) return;
 *
 *     for (LootPoolPickEvent.Candidate c : e.getCandidates()) {
 *         if (!(c.getEntry() instanceof ItemEntry item)) continue;
 *         if (!"legendary_sword".equals(item.getItemId())) continue;
 *         if (hasVipPerk(player)) {
 *             c.setWeight(c.getWeight() * 2);
 *         }
 *     }
 * }
 * }</pre>
 */
public class LootPoolPickEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull LootTable table;
    private final @NotNull LootPool pool;
    private final @NotNull LootContext context;
    private final @NotNull List<Candidate> candidates;

    public LootPoolPickEvent(
            @NotNull LootTable table,
            @NotNull LootPool pool,
            @NotNull LootContext context,
            @NotNull List<Candidate> candidates
    ) {
        this.table = table;
        this.pool = pool;
        this.context = context;
        this.candidates = Collections.unmodifiableList(candidates);
    }

    /**
     * Whichever table this pool actually belongs to — the root table, or a nested one reached
     * via a {@code NestedTableEntry}. {@link LootTable#getId()} identifies it (root or nested,
     * across recursion) without needing to compare instances.
     */
    public @NotNull LootTable getTable() {
        return table;
    }

    public @NotNull LootPool getPool() {
        return pool;
    }

    public @NotNull LootContext getContext() {
        return context;
    }

    /**
     * One entry per pool entry, in pool order — see {@link Candidate}. The list itself is
     * fixed (no adding/removing candidates), but each {@link Candidate}'s weight/eligibility is
     * mutable.
     */
    public @NotNull @Unmodifiable List<Candidate> getCandidates() {
        return candidates;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * One pool entry's pick-time state for this roll iteration. {@code weight}/{@code eligible}
     * start out as whatever the entry's own JSON {@code conditions} already computed; a listener
     * can freely override either before the weighted pick runs.
     */
    public static final class Candidate {
        private final @NotNull LootEntry entry;
        private int weight;
        private boolean eligible;

        public Candidate(@NotNull LootEntry entry, int weight, boolean eligible) {
            this.entry = entry;
            this.weight = weight;
            this.eligible = eligible;
        }

        public @NotNull LootEntry getEntry() {
            return entry;
        }

        /** Effective weight for this pick — 0/negative means "won't be picked". */
        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        /** Whether this entry is in the running at all for this pick. */
        public boolean isEligible() {
            return eligible;
        }

        public void setEligible(boolean eligible) {
            this.eligible = eligible;
        }
    }
}
