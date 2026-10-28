package com.roguesmp.action;

import com.roguesmp.codec.Codec;

import java.util.List;

public record ActionEntry(ClickTrigger click, PlayerAction action) {

    private static final Codec<ActionEntry> FULL_CODEC = Codec.composite(
            Codec.enumOf(ClickTrigger.class).optionalFieldOf("click", ClickTrigger.ANY).forGetter(ActionEntry::click),
            PlayerAction.CODEC.fieldOf("action").forGetter(ActionEntry::action),
            ActionEntry::new
    );

    public static final Codec<ActionEntry> CODEC = Codec.withAlternative(
            FULL_CODEC,
            PlayerAction.CODEC.xmap(action -> new ActionEntry(ClickTrigger.ANY, action), ActionEntry::action)
    );

    public static boolean runAll(List<ActionEntry> entries, ActionContext context) {
        boolean ran = false;
        for (ActionEntry entry : entries) {
            if (!entry.click().matches(context.rightClick())) continue;
            ran |= entry.action().run(context);
        }
        return ran;
    }
}
