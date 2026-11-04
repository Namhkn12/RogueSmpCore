package com.roguesmp.dungeon.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.script.DungeonValidation;
import com.roguesmp.dungeon.script.ScriptContext;
import com.roguesmp.dungeon.script.expr.Statements;
import com.roguesmp.registry.Registries;

import java.util.List;

public interface DungeonAction {

    Codec<DungeonAction> CODEC = Codec.stringOr(
            Codec.dispatch(DungeonAction::getTypeId, Registries.DUNGEON_ACTION_CODEC::getOrThrow),
            Statements.CODEC.xmap(DungeonActions.Eval::new, action -> ((DungeonActions.Eval) action).statements())
    );

    Codec<List<DungeonAction>> LIST_CODEC = Codec.listOrSingle(CODEC);

    static void runAll(List<DungeonAction> actions, ScriptContext context) {
        for (DungeonAction action : actions) action.run(context);
    }

    static void validateAll(List<DungeonAction> actions, DungeonValidation check) {
        for (int i = 0; i < actions.size(); i++) actions.get(i).validate(check.at("[" + i + "]"));
    }

    String getTypeId();

    void run(ScriptContext context);

    default void validate(DungeonValidation check) {
    }
}
