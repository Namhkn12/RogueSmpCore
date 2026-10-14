package com.roguesmp.dungeon;

import com.roguesmp.codec.Codec;

import java.util.Map;

public record EventMatcher(String event, String subject, Map<String, String> data) {

    private static final Codec<EventMatcher> FULL_CODEC = Codec.composite(
            Codec.STRING.fieldOf("event").forGetter(EventMatcher::event),
            Codec.STRING.optionalFieldOf("subject", "").forGetter(EventMatcher::subject),
            Codec.unboundedMap(Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(EventMatcher::data),
            EventMatcher::new
    );

    public static final Codec<EventMatcher> CODEC = Codec.withAlternative(
            FULL_CODEC,
            Codec.STRING.xmap(event -> new EventMatcher(event, "", Map.of()), EventMatcher::event)
    );

    public boolean matches(FeatureContext<?> context, DungeonEvent dungeonEvent) {
        if (dungeonEvent.cancelled() || !event.equals(dungeonEvent.id())) return false;
        if (!subject.isEmpty() && !subject.equals(dungeonEvent.subject())) return false;
        Map<String, String> eventData = dungeonEvent.data();
        return data.entrySet().stream().allMatch(entry -> entry.getValue().equals(eventData.get(entry.getKey())));
    }
}
