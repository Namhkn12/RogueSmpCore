package com.roguesmp.dungeon_v2.utils_.filterchain.impl;

import com.roguesmp.dungeon_v2.utils_.filterchain.EventFilter;
import org.bukkit.GameMode;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;

public class PlayerFilters {

    public static <E extends Event & Cancellable> EventFilter<E> notCancelled() {
        return event -> !event.isCancelled();
    }

    public static EventFilter<PlayerEvent> hasPermission(String permission) {
        return event -> event.getPlayer().hasPermission(permission);
    }

    public static EventFilter<PlayerEvent> isGameMode(GameMode gameMode) {
        return event -> event.getPlayer().getGameMode() == gameMode;
    }

    public static EventFilter<PlayerEvent> isSneaking() {
        return event -> event.getPlayer().isSneaking();
    }

    public static EventFilter<PlayerEvent> isSprinting() {
        return event -> event.getPlayer().isSprinting();
    }

    public static EventFilter<PlayerEvent> inWorld(String worldName) {
        return event -> event.getPlayer().getWorld().getName().equals(worldName);
    }

    public static EventFilter<PlayerEvent> inWorldStartsWith(String prefix) {
        return event -> event.getPlayer().getWorld().getName().startsWith(prefix);
    }
}
