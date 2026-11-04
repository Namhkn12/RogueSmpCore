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
import com.roguesmp.dungeon.script.ScriptContext;
import com.roguesmp.dungeon.script.expr.Expression;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record TriggersFeature(List<Trigger> triggers) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "triggers";

    private static final DataValue FIRED = DataValue.of(true);
    private static final String TIMER_EVENT = "timer";

    // Exactly one of "on" (an event) or "when" (fires when the expression turns true). "recheck" also re-tests "when" every N ticks.
    public record Trigger(Optional<EventMatcher> on, Optional<Expression> when, List<DungeonCondition> conditions, boolean once, int cooldown, int interval, int recheck, List<DungeonAction> actions) {
        public static final Codec<Trigger> CODEC = Codec.composite(
                EventMatcher.CODEC.optionalFieldOf("on").forGetter(Trigger::on),
                Expression.CODEC.optionalFieldOf("when").forGetter(Trigger::when),
                DungeonCondition.LIST_CODEC.optionalFieldOf("if", List.of(), "conditions").forGetter(Trigger::conditions),
                Codec.BOOLEAN.optionalFieldOf("once", false).forGetter(Trigger::once),
                Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Trigger::cooldown),
                Codec.INT.optionalFieldOf("interval", 20).forGetter(Trigger::interval),
                Codec.INT.optionalFieldOf("recheck", 0).forGetter(Trigger::recheck),
                DungeonAction.LIST_CODEC.fieldOf("do", "actions").forGetter(Trigger::actions),
                Trigger::new
        );

        boolean isTimer() {
            return on.isPresent() && TIMER_EVENT.equals(on.get().event());
        }

        boolean watches(DungeonEvent event) {
            return when.isPresent() && event instanceof DungeonEvent.VarChanged changed && when.get().refs().vars().contains(changed.var());
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
            if (trigger.on().isPresent() == trigger.when().isPresent()) {
                at.warn("needs exactly one of 'on' or 'when'");
                continue;
            }
            DungeonValidation scoped = trigger.on().map(on -> at.inEvent(on.event())).orElseGet(at::withoutEvent);
            trigger.on().ifPresent(on -> at.at("on").listens(on));
            trigger.when().ifPresent(when -> {
                scoped.at("when").reads(when.refs());
                if (!when.refs().queries().isEmpty() && trigger.recheck() <= 0) at.at("when").warn("uses queries, which don't re-check it on their own; add \"recheck\": <ticks>");
                else if (when.refs().vars().isEmpty() && trigger.recheck() <= 0) at.at("when").warn("reads no v. variables, so it is only checked when the run starts; add \"recheck\": <ticks>");
            });
            DungeonCondition.validateAll(trigger.conditions(), scoped.at("if"));
            DungeonAction.validateAll(trigger.actions(), scoped.at("do"));
            if (trigger.once()) at.writesVar(featureId + "." + i);
            if (trigger.interval() <= 0) at.warn("'interval' must be positive");
            if (trigger.interval() != 20 && !trigger.isTimer()) at.warn("'interval' only applies to the 'timer' event");
            if (trigger.recheck() < 0) at.warn("'recheck' can't be negative");
            if (trigger.recheck() > 0 && trigger.when().isEmpty()) at.warn("'recheck' only applies to 'when' triggers");
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
    public void onStart(FeatureContext<NoState> context) {
        checkAllWhen(context);
    }

    @Override
    public void onRestore(FeatureContext<NoState> context) {
        checkAllWhen(context);
    }

    @Override
    public void onGameTick(FeatureContext<NoState> context, World world) {
        long ticks = world.getGameTime();
        DungeonEvent.Timer event = null;
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            if (trigger.when().isPresent() && trigger.recheck() > 0 && ticks % trigger.recheck() == 0) {
                checkWhen(context, i, trigger);
                continue;
            }
            if (!trigger.isTimer() || trigger.interval() <= 0 || ticks % trigger.interval() != 0) continue;
            if (event == null) event = new DungeonEvent.Timer(ticks);
            fire(context, i, trigger, event);
        }
    }

    @Override
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            if (trigger.watches(event)) checkWhen(context, i, trigger);
            else if (!trigger.isTimer() && trigger.on().isPresent() && trigger.on().get().matches(event)) fire(context, i, trigger, event);
        }
    }

    private void checkAllWhen(FeatureContext<NoState> context) {
        for (int i = 0; i < triggers.size(); i++) {
            if (triggers.get(i).when().isPresent()) checkWhen(context, i, triggers.get(i));
        }
    }

    private void checkWhen(FeatureContext<NoState> context, int index, Trigger trigger) {
        String stateVar = context.featureId() + "." + index + ".when";
        boolean now = trigger.when().orElseThrow().test(ScriptContext.of(context, (DungeonEvent) null));
        boolean before = FIRED.equals(context.instance().variable(stateVar));
        if (now == before) return;
        context.instance().setVariable(stateVar, DataValue.of(now));
        context.save();
        if (now) fire(context, index, trigger, null);
    }

    private void fire(FeatureContext<NoState> context, int index, Trigger trigger, @Nullable DungeonEvent event) {
        String onceVar = context.featureId() + "." + index;
        String cooldownVar = onceVar + ".cooldown";
        long now = System.currentTimeMillis();
        if (trigger.once() && FIRED.equals(context.instance().variable(onceVar))) return;
        if (trigger.cooldown() > 0 && context.instance().variable(cooldownVar) instanceof DataValue.NumberValue(double until) && now < until) return;

        ScriptContext script = ScriptContext.of(context, event);
        if (!DungeonCondition.all(trigger.conditions(), script)) return;

        if (trigger.once()) {
            context.instance().setVariable(onceVar, FIRED);
            context.save();
        }
        if (trigger.cooldown() > 0) context.instance().setVariable(cooldownVar, DataValue.of(now + trigger.cooldown() * 50L));
        DungeonAction.runAll(trigger.actions(), script);
    }
}
