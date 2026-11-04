package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.region.ServerRegionManager;
import org.bukkit.Location;

import java.util.LinkedHashMap;
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

    // <event> [subject] [@ x y z] [in <region>] [key=value ...]
    public static final Codec<EventMatcher> CODEC = Codec.stringOr(
            FULL_CODEC,
            Codec.STRING.comapFlatMap(EventMatcher::parse, EventMatcher::event)
    );

    public static DataResult<EventMatcher> parse(String source) {
        String[] tokens = source.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) return DataResult.error("Empty event");

        Optional<ValuePredicate> subject = Optional.empty();
        Optional<String> region = Optional.empty();
        Map<String, ValuePredicate> data = new LinkedHashMap<>();
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.equals("@")) {
                if (i + 3 >= tokens.length) return DataResult.error("'@' needs x y z in '" + source + "'");
                String[] axes = {"x", "y", "z"};
                for (String axis : axes) {
                    String coordinate = tokens[++i];
                    try {
                        data.put(axis, new ValuePredicate.Equals(DataValue.of(Integer.parseInt(coordinate))));
                    } catch (NumberFormatException e) {
                        return DataResult.error("'" + coordinate + "' is not a whole number in '" + source + "'");
                    }
                }
            } else if (token.equals("in")) {
                if (i + 1 >= tokens.length) return DataResult.error("'in' needs a region in '" + source + "'");
                region = Optional.of(tokens[++i]);
            } else if (token.indexOf('=') > 0) {
                int split = token.indexOf('=');
                data.put(token.substring(0, split), new ValuePredicate.Equals(literal(token.substring(split + 1))));
            } else if (subject.isEmpty() && i == 1) {
                subject = Optional.of(new ValuePredicate.Equals(DataValue.of(token)));
            } else {
                return DataResult.error("Unexpected '" + token + "' in '" + source + "'");
            }
        }
        return DataResult.success(new EventMatcher(tokens[0], subject, Map.copyOf(data), region));
    }

    private static DataValue literal(String text) {
        if (text.equalsIgnoreCase("true")) return DataValue.of(true);
        if (text.equalsIgnoreCase("false")) return DataValue.of(false);
        try {
            return DataValue.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return DataValue.of(text.length() >= 2 && text.startsWith("'") && text.endsWith("'") ? text.substring(1, text.length() - 1) : text);
        }
    }

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
