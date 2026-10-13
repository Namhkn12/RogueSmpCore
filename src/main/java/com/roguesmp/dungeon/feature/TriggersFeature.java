package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.action.DungeonAction;
import com.roguesmp.dungeon.EventMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TriggersFeature(List<Trigger> triggers) implements DungeonFeature<TriggersFeature.State> {

    public static final String TYPE_KEY = "triggers";

    public record Trigger(EventMatcher on, int count, boolean once, List<DungeonAction> actions) {
        public static final Codec<Trigger> CODEC = Codec.composite(
                EventMatcher.CODEC.fieldOf("on").forGetter(Trigger::on),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Trigger::count),
                Codec.BOOLEAN.optionalFieldOf("once", false).forGetter(Trigger::once),
                Codec.listOf(DungeonAction.CODEC).fieldOf("actions").forGetter(Trigger::actions),
                Trigger::new
        );
    }

    public static final class State {
        public static final Codec<State> CODEC = Codec.composite(
                Codec.listOf(Codec.INT).<Set<Integer>>xmap(HashSet::new, ArrayList::new).optionalFieldOf("fired", Set.of()).forGetter(state -> state.fired),
                Codec.unboundedMap(Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(state -> state.counts),
                State::new
        );

        private final Set<Integer> fired = new HashSet<>();
        private final Map<String, Integer> counts = new HashMap<>();

        public State(Set<Integer> fired, Map<String, Integer> counts) {
            this.fired.addAll(fired);
            this.counts.putAll(counts);
        }
    }

    public static final Codec<TriggersFeature> CODEC = Codec.composite(
            Codec.listOf(Trigger.CODEC).fieldOf("triggers").forGetter(TriggersFeature::triggers),
            TriggersFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State(Set.of(), Map.of());
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onEvent(FeatureContext<State> context, DungeonEvent event) {
        State state = context.state();
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            if (state.fired.contains(i) || !trigger.on().matches(context, event)) continue;

            if (trigger.count() > 1) {
                String key = String.valueOf(i);
                int seen = state.counts.merge(key, 1, Integer::sum);
                if (seen < trigger.count()) continue;
                state.counts.remove(key);
            }
            if (trigger.once()) state.fired.add(i);
            if (trigger.once() || trigger.count() > 1) context.save();
            trigger.actions().forEach(action -> action.run(context, event.player()));
        }
    }
}
