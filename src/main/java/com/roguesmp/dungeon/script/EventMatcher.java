package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.region.ServerRegionManager;
import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;

public record EventMatcher(String event, Optional<ValuePredicate> subject, Map<String, ValuePredicate> data, Optional<String> region) {

    private static final Codec<EventMatcher> FULL_CODEC = Codec.composite(
            Codec.STRING.fieldOf("event").forGetter(EventMatcher::event),
            ValuePredicate.CODEC.optionalFieldOf("subject").forGetter(EventMatcher::subject),
            Codec.unboundedMap(ValuePredicate.CODEC).optionalFieldOf("data", Map.of()).forGetter(EventMatcher::data),
            Codec.STRING.optionalFieldOf("region").forGetter(EventMatcher::region),
            EventMatcher::new
    );

    public static final Codec<EventMatcher> CODEC = Codec.withAlternative(
            FULL_CODEC,
            Codec.STRING.xmap(event -> new EventMatcher(event, Optional.empty(), Map.of(), Optional.empty()), EventMatcher::event)
    );

    public boolean matches(DungeonEvent dungeonEvent) {
        if (dungeonEvent.cancelled() || !event.equals(dungeonEvent.id())) return false;
        if (subject.isPresent() && !subject.get().test(DataValue.of(dungeonEvent.subject()))) return false;
        if (region.isPresent()) {
            Location location = dungeonEvent.location();
            if (location == null || !ServerRegionManager.getInstance().regionIdsAt(location).contains(region.get())) return false;
        }
        Map<String, DataValue> eventData = dungeonEvent.data();
        return data.entrySet().stream().allMatch(entry -> entry.getValue().test(eventData.get(entry.getKey())));
    }
}
