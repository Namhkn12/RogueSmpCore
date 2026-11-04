package com.roguesmp.dungeon.script.expr;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.ScriptContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Statements {

    public static final Codec<Statements> CODEC = Codec.STRING.comapFlatMap(Statements::compile, Statements::source);

    enum Operator { SET, ADD, SUBTRACT, MULTIPLY, DIVIDE }

    record Assignment(ExprParser.Scope scope, String name, Operator operator, Expr value) {
        void run(ScriptContext context) {
            DataValue result = value.eval(context);
            if (result == null) result = Expr.ZERO;
            if (operator != Operator.SET) {
                double current = Expr.number(read(context));
                double amount = Expr.number(result);
                double combined = switch (operator) {
                    case ADD -> current + amount;
                    case SUBTRACT -> current - amount;
                    case MULTIPLY -> current * amount;
                    case DIVIDE -> amount == 0 ? 0 : current / amount;
                    case SET -> amount;
                };
                result = DataValue.of(combined);
            }
            if (scope == ExprParser.Scope.TEMP) context.temps().put(name, result);
            else context.setVariable(name, scope == ExprParser.Scope.PLAYER_VARIABLE, result);
        }

        private DataValue read(ScriptContext context) {
            return scope == ExprParser.Scope.TEMP ? context.temps().get(name) : context.variable(name, scope == ExprParser.Scope.PLAYER_VARIABLE);
        }
    }

    private final String source;
    private final List<Assignment> assignments;
    private final ExprRefs refs;

    private Statements(String source, List<Assignment> assignments, ExprRefs refs) {
        this.source = source;
        this.assignments = assignments;
        this.refs = refs;
    }

    public static DataResult<Statements> compile(String source) {
        try {
            ExprParser.Parsed<List<Assignment>> parsed = ExprParser.statements(source);
            return DataResult.success(new Statements(source, List.copyOf(parsed.value()), parsed.refs()));
        } catch (ExprParser.ParseException e) {
            return DataResult.error("Bad statement '" + source + "': " + e.getMessage());
        }
    }

    public String source() {
        return source;
    }

    public ExprRefs reads() {
        return refs;
    }

    public Set<String> writes() {
        return assignments.stream().filter(assignment -> assignment.scope() != ExprParser.Scope.TEMP).map(Assignment::name).collect(Collectors.toSet());
    }

    public void run(ScriptContext context) {
        for (Assignment assignment : assignments) assignment.run(context);
    }
}
