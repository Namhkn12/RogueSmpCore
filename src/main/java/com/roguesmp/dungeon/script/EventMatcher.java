package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonEvent;

import java.util.Map;
import java.util.Optional;

public record EventMatcher(String event, Optional<ValuePredicate> subject, Map<String, ValuePredicate> data) {

    private static final Codec<EventMatcher> FULL_CODEC = Codec.composite(
            Codec.STRING.fieldOf("event").forGetter(EventMatcher::event),
            ValuePredicate.CODEC.optionalFieldOf("subject").forGetter(EventMatcher::subject),
            Codec.unboundedMap(ValuePredicate.CODEC).optionalFieldOf("data", Map.of()).forGetter(EventMatcher::data),
            EventMatcher::new
    );

    public static final Codec<EventMatcher> CODEC = Codec.withAlternative(
            FULL_CODEC,
            Codec.STRING.xmap(event -> new EventMatcher(event, Optional.empty(), Map.of()), EventMatcher::event)
    );

    public boolean matches(DungeonEvent dungeonEvent) {
        if (dungeonEvent.cancelled() || !event.equals(dungeonEvent.id())) return false;
        if (subject.isPresent() && !subject.get().test(DataValue.of(dungeonEvent.subject()))) return false;
        Map<String, DataValue> eventData = dungeonEvent.data();
        return data.entrySet().stream().allMatch(entry -> entry.getValue().test(eventData.get(entry.getKey())));
    }
}
