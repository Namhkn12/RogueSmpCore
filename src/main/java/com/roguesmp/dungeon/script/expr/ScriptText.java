package com.roguesmp.dungeon.script.expr;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.script.ScriptContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ScriptText {

    public static final Codec<ScriptText> CODEC = Codec.STRING.comapFlatMap(ScriptText::compile, ScriptText::source);
    public static final ScriptText EMPTY = new ScriptText("", List.of(), ExprRefs.EMPTY);

    private static final Pattern PLACEHOLDER = Pattern.compile("[A-Za-z_]+");

    private sealed interface Part {
    }

    private record Literal(String text) implements Part {
    }

    private record Placeholder(String name) implements Part {
    }

    private record Value(Expression expression) implements Part {
    }

    private final String source;
    private final List<Part> parts;
    private final ExprRefs refs;

    private ScriptText(String source, List<Part> parts, ExprRefs refs) {
        this.source = source;
        this.parts = parts;
        this.refs = refs;
    }

    public static DataResult<ScriptText> compile(String source) {
        List<Part> parts = new ArrayList<>();
        ExprRefs refs = ExprRefs.EMPTY;
        int i = 0;
        while (i < source.length()) {
            int open = source.indexOf('{', i);
            if (open < 0) {
                parts.add(new Literal(source.substring(i)));
                break;
            }
            int close = source.indexOf('}', open);
            if (close < 0) return DataResult.error("Unclosed '{' in text '" + source + "'");
            if (open > i) parts.add(new Literal(source.substring(i, open)));
            String inside = source.substring(open + 1, close).trim();
            if (PLACEHOLDER.matcher(inside).matches()) {
                parts.add(new Placeholder(inside));
            } else {
                DataResult<Expression> expression = Expression.compile(inside);
                if (!expression.isSuccess()) return DataResult.error(expression.error() + " in text '" + source + "'");
                parts.add(new Value(expression.result()));
                refs = refs.merge(expression.result().refs());
            }
            i = close + 1;
        }
        return DataResult.success(new ScriptText(source, List.copyOf(parts), refs));
    }

    public String source() {
        return source;
    }

    public ExprRefs refs() {
        return refs;
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }

    public String format(ScriptContext context) {
        StringBuilder builder = new StringBuilder();
        for (Part part : parts) {
            switch (part) {
                case Literal(String text) -> builder.append(text);
                case Value(Expression expression) -> builder.append(expression.display(context));
                case Placeholder(String name) -> {
                    if (name.equals("player")) builder.append(context.player() == null ? "" : context.player().getName());
                    else builder.append('{').append(name).append('}');
                }
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return source;
    }
}
