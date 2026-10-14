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

import java.util.List;

public record TriggersFeature(List<Trigger> triggers) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "triggers";

    private static final DataValue FIRED = DataValue.of(true);

    public record Trigger(EventMatcher on, List<DungeonCondition> conditions, boolean once, List<DungeonAction> actions) {
        public static final Codec<Trigger> CODEC = Codec.composite(
                EventMatcher.CODEC.fieldOf("on").forGetter(Trigger::on),
                Codec.listOf(DungeonCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(Trigger::conditions),
                Codec.BOOLEAN.optionalFieldOf("once", false).forGetter(Trigger::once),
                Codec.listOf(DungeonAction.CODEC).fieldOf("actions").forGetter(Trigger::actions),
                Trigger::new
        );
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
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        for (int i = 0; i < triggers.size(); i++) {
            Trigger trigger = triggers.get(i);
            String onceVar = context.featureId() + "." + i;
            if (!trigger.on().matches(event)) continue;
            if (trigger.once() && FIRED.equals(context.instance().variable(onceVar))) continue;
            if (!DungeonCondition.all(trigger.conditions(), context, event)) continue;

            if (trigger.once()) {
                context.instance().setVariable(onceVar, FIRED);
                context.save();
            }
            trigger.actions().forEach(action -> action.run(context, event.player()));
        }
    }
}
