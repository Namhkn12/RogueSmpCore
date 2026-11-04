package com.roguesmp.dungeon.listener;

import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.script.DataValue;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// Turns one Bukkit event into dungeon events. "always" adapters feed features and listen regardless of scripts.
public final class BukkitEventAdapter<E extends Event> {

    public record Emits(String id, Set<String> dataKeys, boolean cancellable) {
    }

    public interface Emitter {
        void fire(@Nullable World world, DungeonEvent event);

        void fire(@Nullable DungeonInstance instance, DungeonEvent event);
    }

    @FunctionalInterface
    public interface Translator<E> {
        void translate(E event, Emitter emit);
    }

    private final String name;
    private final Class<E> type;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final boolean always;
    private final List<Emits> emits;
    private final Translator<E> translator;

    private BukkitEventAdapter(String name, Class<E> type, EventPriority priority, boolean ignoreCancelled, boolean always, List<Emits> emits, Translator<E> translator) {
        this.name = name;
        this.type = type;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.always = always;
        this.emits = List.copyOf(emits);
        this.translator = translator;
    }

    public static <E extends Event> Builder<E> of(String id, Class<E> type) {
        return new Builder<>(id, type);
    }

    public static <E extends Event> CustomBuilder<E> custom(String name, Class<E> type) {
        return new CustomBuilder<>(name, type);
    }

    public String name() {
        return name;
    }

    public Class<E> type() {
        return type;
    }

    public EventPriority priority() {
        return priority;
    }

    public boolean ignoreCancelled() {
        return ignoreCancelled;
    }

    public boolean always() {
        return always;
    }

    public List<Emits> emits() {
        return emits;
    }

    void translate(Event event, Emitter emit) {
        if (type.isInstance(event)) translator.translate(type.cast(event), emit);
    }

    // Reads a script-only event straight off the Bukkit event; it reaches scripts as DungeonEvent.External.
    public record Simple<E extends Event>(String id, Class<E> type, Function<E, @Nullable Player> player,
                                          Function<E, @Nullable Location> location, Function<E, String> subject,
                                          Map<String, Function<E, DataValue>> data) {

        public @Nullable Player playerOf(Event event) {
            return player.apply(type.cast(event));
        }

        public @Nullable Location locationOf(Event event) {
            Location at = location.apply(type.cast(event));
            if (at != null) return at;
            Player source = playerOf(event);
            return source == null ? null : source.getLocation();
        }

        public String subjectOf(Event event) {
            return subject.apply(type.cast(event));
        }

        public Map<String, DataValue> dataOf(Event event) {
            E typed = type.cast(event);
            Map<String, DataValue> values = new HashMap<>();
            data.forEach((key, extractor) -> {
                DataValue value = extractor.apply(typed);
                if (value != null) values.put(key, value);
            });
            return values;
        }
    }

    public static final class Builder<E extends Event> {
        private final String id;
        private final Class<E> type;
        private EventPriority priority = EventPriority.NORMAL;
        private Function<E, @Nullable Player> player = event -> null;
        private Function<E, @Nullable Location> location = event -> null;
        private Function<E, String> subject = event -> "";
        private final Map<String, Function<E, DataValue>> data = new LinkedHashMap<>();

        private Builder(String id, Class<E> type) {
            this.id = id;
            this.type = type;
        }

        public Builder<E> priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder<E> player(Function<E, @Nullable Player> player) {
            this.player = player;
            return this;
        }

        public Builder<E> location(Function<E, @Nullable Location> location) {
            this.location = location;
            return this;
        }

        public Builder<E> subject(Function<E, String> subject) {
            this.subject = subject;
            return this;
        }

        public Builder<E> data(String key, Function<E, DataValue> extractor) {
            data.put(key, extractor);
            return this;
        }

        public BukkitEventAdapter<E> build() {
            Simple<E> simple = new Simple<>(id, type, player, location, subject, Map.copyOf(data));
            Emits emits = new Emits(id, Set.copyOf(data.keySet()), Cancellable.class.isAssignableFrom(type));
            return new BukkitEventAdapter<>(id, type, priority, true, false, List.of(emits), (event, emit) -> {
                Location at = simple.locationOf(event);
                emit.fire(at == null ? null : at.getWorld(), new DungeonEvent.External(simple, event));
            });
        }
    }

    public static final class CustomBuilder<E extends Event> {
        private final String name;
        private final Class<E> type;
        private EventPriority priority = EventPriority.MONITOR;
        private boolean ignoreCancelled;
        private boolean always;
        private final List<Emits> emits = new ArrayList<>();

        private CustomBuilder(String name, Class<E> type) {
            this.name = name;
            this.type = type;
        }

        public CustomBuilder<E> priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }

        public CustomBuilder<E> ignoreCancelled() {
            this.ignoreCancelled = true;
            return this;
        }

        public CustomBuilder<E> always() {
            this.always = true;
            return this;
        }

        public CustomBuilder<E> emits(String id, String... dataKeys) {
            emits.add(new Emits(id, Set.of(dataKeys), false));
            return this;
        }

        public CustomBuilder<E> emitsCancellable(String id, String... dataKeys) {
            emits.add(new Emits(id, Set.of(dataKeys), true));
            return this;
        }

        public BukkitEventAdapter<E> translate(Translator<E> translator) {
            return new BukkitEventAdapter<>(name, type, priority, ignoreCancelled, always, emits, translator);
        }
    }
}
