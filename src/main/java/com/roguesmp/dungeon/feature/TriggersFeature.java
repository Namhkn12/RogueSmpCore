package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.NoState;
import com.roguesmp.dungeon.action.DungeonAction;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.DungeonCondition;
import com.roguesmp.dungeon.script.DungeonValidation;
import com.roguesmp.dungeon.script.EventMatcher;

import org.bukkit.World;

import java.util.List;

public record TriggersFeature(List<Trigger> triggers) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "triggers";

    private static final DataValue FIRED = DataValue.of(true);
    private static final String TIMER_EVENT = "timer";

    public record Trigger(EventMatcher on, List<DungeonCondition> conditions, boolean once, int cooldown, int interval, List<DungeonAction> actions) {
        public static final Codec<Trigger> CODEC = Codec.composite(
                EventMatcher.CODEC.fieldOf("on").forGetter(Trigger::on),
                Codec.listOf(DungeonCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(Trigger::conditions),
                Codec.BOOLEAN.optionalFieldOf("once", false).forGetter(Trigger::once),
                Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Trigger::cooldown),
                Codec.INT.optionalFieldOf("interval", 20).forGetter(Trigger::interval),
                Codec.listOf(DungeonAction.CODEC).fieldOf("actions").forGetter(Trigger::actions),
                Trigger::new
        );

        boolean isTimer() {
            return TIMER_EVENT.equals(on.event());
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
    public void validate(String featureId, DungeonValidation check) {
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            DungeonValidation at = check.at("triggers[" + i + "]");
            at.at("on").listens(trigger.on());
            for (int j = 0; j < trigger.conditions().size(); j++) trigger.conditions().get(j).validate(at.at("conditions[" + j + "]"));
            for (int j = 0; j < trigger.actions().size(); j++) trigger.actions().get(j).validate(at.at("actions[" + j + "]"));
            if (trigger.once()) at.writesVar(featureId + "." + i);
            if (trigger.interval() <= 0) at.warn("'interval' must be positive");
            if (trigger.interval() != 20 && !trigger.isTimer()) at.warn("'interval' only applies to the 'timer' event");
        }
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }

    @Override
    public void onGameTick(FeatureContext<NoState> context, World world) {
        long ticks = world.getGameTime();
        DungeonEvent.Timer event = null;
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            if (!trigger.isTimer() || trigger.interval() <= 0 || ticks % trigger.interval() != 0) continue;
            if (event == null) event = new DungeonEvent.Timer(ticks);
            fire(context, i, trigger, event);
        }
    }

    @Override
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            if (!trigger.isTimer()) fire(context, i, trigger, event);
        }
    }

    private void fire(FeatureContext<NoState> context, int index, Trigger trigger, DungeonEvent event) {
        String onceVar = context.featureId() + "." + index;
        String cooldownVar = onceVar + ".cooldown";
        long now = System.currentTimeMillis();
        if (!trigger.on().matches(event)) return;
        if (trigger.once() && FIRED.equals(context.instance().variable(onceVar))) return;
        if (trigger.cooldown() > 0 && context.instance().variable(cooldownVar) instanceof DataValue.NumberValue(double until) && now < until) return;
        if (!DungeonCondition.all(trigger.conditions(), context, event.player())) return;

        if (trigger.once()) {
            context.instance().setVariable(onceVar, FIRED);
            context.save();
        }
        if (trigger.cooldown() > 0) context.instance().setVariable(cooldownVar, DataValue.of(now + trigger.cooldown() * 50L));
        trigger.actions().forEach(action -> action.run(context, event.player()));
    }
}
