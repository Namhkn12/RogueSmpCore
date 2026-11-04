package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.action.DungeonAction;

import java.util.List;

public record ScriptFunction(List<String> params, List<DungeonAction> actions) {

    public static final Codec<ScriptFunction> CODEC = Codec.withAlternative(
            Codec.composite(
                    Codec.listOf(Codec.STRING).optionalFieldOf("params", List.of()).forGetter(ScriptFunction::params),
                    DungeonAction.LIST_CODEC.fieldOf("do", "actions").forGetter(ScriptFunction::actions),
                    ScriptFunction::new
            ),
            DungeonAction.LIST_CODEC.xmap(actions -> new ScriptFunction(List.of(), actions), ScriptFunction::actions)
    );
}
