package com.roguesmp.dungeon_v2.utils.filterchain;

import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

public class FilterChain<E extends Event> {

    private final List<EventFilter<E>> filters = new ArrayList<>();

    public static <E extends Event> FilterChain<E> of(Class<E> clazz) {
        return new FilterChain<>();
    }

    public FilterChain<E> require(EventFilter<E> filter) {
        filters.add(filter);
        return this;
    }

    public boolean test(E event) {
        for (EventFilter<E> filter : filters) {
            if (!filter.test(event)) return false;
        }
        return true;
    }

    public EventFilter<E> build() {
        List<EventFilter<E>> snapshot = List.copyOf(filters);
        return event -> {
            for (EventFilter<E> filter : snapshot) {
                if (!filter.test(event)) return false;
            }
            return true;
        };
    }
}
