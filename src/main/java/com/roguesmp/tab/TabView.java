package com.roguesmp.tab;

import java.util.UUID;

/** A live scoreboard or tablist view for one player. {@link TabEngine} ticks it every server tick and destroys it when it's no longer needed. */
public interface TabView {

    /** The player this view belongs to. */
    UUID playerId();

    /** Called once per server tick by {@link TabEngine}. This is what drives elements with a positive {@code refreshTicks()}. */
    void tick();

    /** Removes this view and undoes anything it showed the player. Called on manual cleanup, plugin shutdown, or player quit. */
    void destroy();
}
