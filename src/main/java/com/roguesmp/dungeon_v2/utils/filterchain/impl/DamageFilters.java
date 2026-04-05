package com.roguesmp.dungeon_v2.utils.filterchain.impl;

import com.roguesmp.dungeon_v2.utils.filterchain.EventFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageFilters {

    public static EventFilter<EntityDamageByEntityEvent> attackerIsPlayer() {
        return event -> event.getDamager() instanceof Player;
    }

    public static EventFilter<EntityDamageByEntityEvent> victimIsPlayer() {
        return event -> event.getEntity() instanceof Player;
    }

    public static EventFilter<EntityDamageByEntityEvent> cause(EntityDamageEvent.DamageCause cause) {
        return event -> event.getCause() == cause;
    }

    public static EventFilter<EntityDamageByEntityEvent> minDamage(double min) {
        return event -> event.getFinalDamage() >= min;
    }

    public static EventFilter<EntityDamageByEntityEvent> attackerInWorld(String prefix) {
        return event -> event.getDamager().getWorld().getName().startsWith(prefix);
    }
}