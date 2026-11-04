package com.roguesmp.dungeon.script.expr;

import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.ScriptContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public record ExprFunction(int minArgs, int maxArgs, boolean needsPlayer, Body body) {

    @FunctionalInterface
    public interface Body {
        @Nullable DataValue eval(ScriptContext context, List<DataValue> args);
    }

    public static ExprFunction of(int args, Body body) {
        return new ExprFunction(args, args, false, body);
    }

    public static ExprFunction of(int minArgs, int maxArgs, Body body) {
        return new ExprFunction(minArgs, maxArgs, false, body);
    }

    public static ExprFunction player(int minArgs, int maxArgs, Body body) {
        return new ExprFunction(minArgs, maxArgs, true, body);
    }

    static double number(List<DataValue> args, int index) {
        return Expr.number(args.get(index));
    }

    static String string(List<DataValue> args, int index) {
        return Expr.display(args.get(index));
    }

    private static final Map<String, ExprFunction> MATH = Map.ofEntries(
            Map.entry("abs", unary(Math::abs)),
            Map.entry("floor", unary(Math::floor)),
            Map.entry("ceil", unary(Math::ceil)),
            Map.entry("round", unary(value -> Math.round(value))),
            Map.entry("trunc", unary(value -> (long) value)),
            Map.entry("sqrt", unary(value -> value < 0 ? 0 : Math.sqrt(value))),
            Map.entry("min", binary(Math::min)),
            Map.entry("max", binary(Math::max)),
            Map.entry("pow", binary(Math::pow)),
            Map.entry("mod", binary((a, b) -> b == 0 ? 0 : a % b)),
            Map.entry("clamp", ExprFunction.of(3, (context, args) -> DataValue.of(Math.clamp(number(args, 0), Math.min(number(args, 1), number(args, 2)), Math.max(number(args, 1), number(args, 2)))))),
            Map.entry("lerp", ExprFunction.of(3, (context, args) -> DataValue.of(number(args, 0) + (number(args, 1) - number(args, 0)) * number(args, 2)))),
            Map.entry("random", ExprFunction.of(2, (context, args) -> {
                double low = Math.min(number(args, 0), number(args, 1));
                double high = Math.max(number(args, 0), number(args, 1));
                return DataValue.of(low == high ? low : ThreadLocalRandom.current().nextDouble(low, high));
            })),
            Map.entry("random_integer", ExprFunction.of(2, (context, args) -> {
                long low = Math.round(Math.min(number(args, 0), number(args, 1)));
                long high = Math.round(Math.max(number(args, 0), number(args, 1)));
                return DataValue.of(ThreadLocalRandom.current().nextLong(low, high + 1));
            }))
    );


    static @Nullable ExprFunction math(String name) {
        return MATH.get(name);
    }

    private static ExprFunction unary(DoubleUnaryOperator operator) {
        return ExprFunction.of(1, (context, args) -> DataValue.of(operator.applyAsDouble(number(args, 0))));
    }

    private static ExprFunction binary(DoubleBinaryOperator operator) {
        return ExprFunction.of(2, (context, args) -> DataValue.of(operator.applyAsDouble(number(args, 0), number(args, 1))));
    }
}
