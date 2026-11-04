package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.script.expr.Expression;
import com.roguesmp.registry.Registries;

import java.util.List;

public interface DungeonCondition {

    Codec<DungeonCondition> CODEC = Codec.stringOr(
            Codec.dispatch(DungeonCondition::getTypeId, Registries.DUNGEON_CONDITION_CODEC::getOrThrow),
            Expression.CODEC.xmap(DungeonConditions.Expr::new, condition -> ((DungeonConditions.Expr) condition).expression())
    );

    Codec<List<DungeonCondition>> LIST_CODEC = Codec.listOrSingle(CODEC);

    static boolean all(List<DungeonCondition> conditions, ScriptContext context) {
        for (DungeonCondition condition : conditions) {
            if (!condition.test(context)) return false;
        }
        return true;
    }

    static void validateAll(List<DungeonCondition> conditions, DungeonValidation check) {
        for (int i = 0; i < conditions.size(); i++) conditions.get(i).validate(check.at("[" + i + "]"));
    }

    String getTypeId();

    boolean test(ScriptContext context);

    default void validate(DungeonValidation check) {
    }
}
