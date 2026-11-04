package com.roguesmp.dungeon.script.expr;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.dungeon.script.ScriptContext;
import org.jetbrains.annotations.Nullable;

// A number field that is either a plain number or an expression string, evaluated when the action runs.
public final class ScriptNumber {

    public static final Codec<ScriptNumber> CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(ScriptNumber input, DynamicOps<O> ops) {
            return DataResult.success(input.expression == null ? ops.createDouble(input.constant) : ops.createString(input.expression.source()));
        }

        @Override
        public <O> DataResult<ScriptNumber> decode(O input, DynamicOps<O> ops) {
            DataResult<String> string = ops.getString(input);
            if (string.isSuccess()) return Expression.compile(string.result()).map(ScriptNumber::new);
            return ops.getDouble(input).map(ScriptNumber::of);
        }
    };

    private final double constant;
    private final @Nullable Expression expression;

    private ScriptNumber(double constant, @Nullable Expression expression) {
        this.constant = constant;
        this.expression = expression;
    }

    private ScriptNumber(Expression expression) {
        this(0, expression);
    }

    public static ScriptNumber of(double value) {
        return new ScriptNumber(value, null);
    }

    public double get(ScriptContext context) {
        return expression == null ? constant : expression.number(context);
    }

    public int getInt(ScriptContext context) {
        return (int) Math.round(get(context));
    }

    public boolean isConstant() {
        return expression == null;
    }

    // Only meaningful when isConstant().
    public double constant() {
        return constant;
    }

    public ExprRefs refs() {
        return expression == null ? ExprRefs.EMPTY : expression.refs();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScriptNumber number && number.constant == constant && number.expression == expression;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(constant);
    }

    @Override
    public String toString() {
        return expression == null ? String.valueOf(constant) : expression.source();
    }
}
