package com.roguesmp.tab;

import java.util.UUID;

/** A live per-player scoreboard or tablist rendering, ticked by {@link TabEngine} and torn down on quit. */
public interface TabView {

    UUID playerId();

    /** Called once per server tick by {@link TabEngine}; drives elements with a positive {@code refreshTicks()}. */
    void tick();

    void destroy();
}
