package com.roguesmp.dungeon.script.expr;

import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.ScriptContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

sealed interface Expr {

    DataValue TRUE = DataValue.of(true);
    DataValue FALSE = DataValue.of(false);
    DataValue ZERO = DataValue.of(0);

    static DataValue of(boolean value) {
        return value ? TRUE : FALSE;
    }

    static boolean truthy(@Nullable DataValue value) {
        return switch (value) {
            case null -> false;
            case DataValue.BooleanValue(boolean bool) -> bool;
            case DataValue.NumberValue(double number) -> number != 0;
            case DataValue.StringValue(String string) -> !string.isEmpty();
        };
    }

    static double number(@Nullable DataValue value) {
        return switch (value) {
            case null -> 0;
            case DataValue.BooleanValue(boolean bool) -> bool ? 1 : 0;
            case DataValue.NumberValue(double number) -> number;
            case DataValue.StringValue(String string) -> {
                try {
                    yield Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
        };
    }

    static String display(@Nullable DataValue value) {
        return value == null ? "0" : value.display();
    }

    static boolean equal(@Nullable DataValue left, @Nullable DataValue right) {
        if (left instanceof DataValue.StringValue || right instanceof DataValue.StringValue) return display(left).equals(display(right));
        return number(left) == number(right);
    }

    @Nullable DataValue eval(ScriptContext context);

    record Constant(DataValue value) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return value;
        }
    }

    record Variable(String name, boolean perPlayer) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            return context.variable(name, perPlayer);
        }
    }

    record Temp(String name) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            return context.temps().get(name);
        }
    }

    record Context(String key) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            return context.context(key);
        }
    }

    record Call(ExprFunction function, List<Expr> args) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            if (function.needsPlayer() && context.player() == null) return null;
            List<DataValue> values = new ArrayList<>(args.size());
            for (Expr arg : args) values.add(arg.eval(context));
            return function.body().eval(context, values);
        }
    }

    record Not(Expr operand) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return of(!truthy(operand.eval(context)));
        }
    }

    record Negate(Expr operand) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return DataValue.of(-number(operand.eval(context)));
        }
    }

    record And(Expr left, Expr right) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return of(truthy(left.eval(context)) && truthy(right.eval(context)));
        }
    }

    record Or(Expr left, Expr right) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return of(truthy(left.eval(context)) || truthy(right.eval(context)));
        }
    }

    record Coalesce(Expr value, Expr fallback) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            DataValue result = value.eval(context);
            return result != null ? result : fallback.eval(context);
        }
    }

    record Ternary(Expr test, Expr then, Expr otherwise) implements Expr {
        @Override
        public @Nullable DataValue eval(ScriptContext context) {
            return truthy(test.eval(context)) ? then.eval(context) : otherwise.eval(context);
        }
    }

    record Equals(Expr left, Expr right, boolean negated) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return of(equal(left.eval(context), right.eval(context)) != negated);
        }
    }

    enum Compare {
        LESS("<"), LESS_EQUAL("<="), GREATER(">"), GREATER_EQUAL(">=");

        final String symbol;

        Compare(String symbol) {
            this.symbol = symbol;
        }

        boolean test(double left, double right) {
            return switch (this) {
                case LESS -> left < right;
                case LESS_EQUAL -> left <= right;
                case GREATER -> left > right;
                case GREATER_EQUAL -> left >= right;
            };
        }
    }

    record Comparison(Compare compare, Expr left, Expr right) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            return of(compare.test(number(left.eval(context)), number(right.eval(context))));
        }
    }

    record Add(Expr left, Expr right) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            DataValue a = left.eval(context);
            DataValue b = right.eval(context);
            if (a instanceof DataValue.StringValue || b instanceof DataValue.StringValue) return DataValue.of(display(a) + display(b));
            return DataValue.of(number(a) + number(b));
        }
    }

    record Arithmetic(DoubleBinaryOperator operator, Expr left, Expr right) implements Expr {
        @Override
        public DataValue eval(ScriptContext context) {
            double result = operator.applyAsDouble(number(left.eval(context)), number(right.eval(context)));
            return DataValue.of(Double.isFinite(result) ? result : 0);
        }
    }
}
