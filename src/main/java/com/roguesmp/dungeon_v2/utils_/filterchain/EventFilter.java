package com.roguesmp.dungeon_v2.utils_.filterchain;

import org.bukkit.event.Event;

public interface EventFilter<E extends Event> {
    boolean test(E event);

    default EventFilter<E> and(EventFilter<E> other){
        return event -> this.test(event) && other.test(event);
    }

    default EventFilter<E> or(EventFilter<E> orther){
        return event -> this.test(event) || orther.test(event);
    }

    default EventFilter<E> negate(){
        return event -> !this.test(event);
    }
}
