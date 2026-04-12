package com.roguesmp.dungeon.utils.filterchain.impl;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.utils.filterchain.EventFilter;
import org.bukkit.entity.Entity;

public class EntityFilters {
    public static <T extends Entity> EventFilter<EntityAddToWorldEvent> entityType(Class<T> type) {
        return e -> type.isInstance(e.getEntity());    }
}
