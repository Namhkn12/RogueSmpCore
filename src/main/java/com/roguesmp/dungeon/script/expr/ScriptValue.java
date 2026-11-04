package com.roguesmp.dungeon.script.expr;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.ScriptContext;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public sealed interface ScriptValue {

    Pattern LOOKS_LIKE_EXPRESSION = Pattern.compile("(?i)\\b(v|p|t|c|q|var|pvar|variable|temp|context|query|math)\\.\\w");

    // A string that parses as an expression is one; any other string stays a plain text value.
    Codec<ScriptValue> CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(ScriptValue input, DynamicOps<O> ops) {
            return switch (input) {
                case Constant(DataValue value, String parseError) -> DataValue.CODEC.encode(value, ops);
                case Computed(Expression expression) -> DataResult.success(ops.createString(expression.source()));
            };
        }

        @Override
        public <O> DataResult<ScriptValue> decode(O input, DynamicOps<O> ops) {
            DataResult<String> string = ops.getString(input);
            if (string.isSuccess()) {
                DataResult<Expression> expression = Expression.compile(string.result());
                return DataResult.success(expression.isSuccess() ? new Computed(expression.result()) : new Constant(DataValue.of(string.result()), expression.error()));
            }
            return DataValue.CODEC.decode(input, ops).map(ScriptValue::of);
        }
    };

    static ScriptValue of(DataValue value) {
        return new Constant(value, null);
    }

    DataValue get(ScriptContext context);

    ExprRefs refs();

    // The parse error of text that was probably meant as an expression, kept as text instead.
    default @Nullable String suspiciousText() {
        return null;
    }

    record Constant(DataValue value, @Nullable String parseError) implements ScriptValue {
        @Override
        public DataValue get(ScriptContext context) {
            return value;
        }

        @Override
        public ExprRefs refs() {
            return ExprRefs.EMPTY;
        }

        @Override
        public @Nullable String suspiciousText() {
            return parseError != null && LOOKS_LIKE_EXPRESSION.matcher(value.display()).find() ? parseError : null;
        }
    }

    record Computed(Expression expression) implements ScriptValue {
        @Override
        public DataValue get(ScriptContext context) {
            return expression.valueOrZero(context);
        }

        @Override
        public ExprRefs refs() {
            return expression.refs();
        }
    }
}
