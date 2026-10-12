package com.roguesmp.tab;

import java.util.UUID;

/** A live scoreboard or tablist view for one player. {@link TabEngine} ticks it every server tick and destroys it when it's no longer needed. */
public interface TabView {

    /** The player this view belongs to. */
    UUID playerId();

    /** Called once per server tick by {@link TabEngine}. This is what drives elements with a positive {@code refreshTicks()}. */
    void tick();

    /**
     * Removes this view. With {@code restoreDefaults}, also puts back what the player sees when no view is
     * showing. Pass {@code false} when another view of the same kind is about to be shown right away.
     */
    void destroy(boolean restoreDefaults);

    /** Removes this view and restores the defaults. Called on manual cleanup, plugin shutdown, or player quit. */
    default void destroy() {
        destroy(true);
    }
}
