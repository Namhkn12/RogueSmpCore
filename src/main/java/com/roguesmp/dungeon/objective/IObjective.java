package com.roguesmp.dungeon.objective;

import org.bukkit.event.EventHandler;

public interface IObjective {
    @EventHandler
    public void start();

    @EventHandler
    public void process();

    @EventHandler
    public void end();
}
