package com.roguesmp.dungeon.script.expr;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.dungeon.script.ScriptContext;
import org.jetbrains.annotations.Nullable;

public final class Expression {

    public static final Codec<Expression> CODEC = Codec.STRING.comapFlatMap(Expression::compile, Expression::source);

    private final String source;
    private final Expr root;
    private final ExprRefs refs;

    private Expression(String source, Expr root, ExprRefs refs) {
        this.source = source;
        this.root = root;
        this.refs = refs;
    }

    public static DataResult<Expression> compile(String source) {
        try {
            ExprParser.Parsed<Expr> parsed = ExprParser.expression(source);
            return DataResult.success(new Expression(source, parsed.value(), parsed.refs()));
        } catch (ExprParser.ParseException e) {
            return DataResult.error("Bad expression '" + source + "': " + e.getMessage());
        }
    }

    public String source() {
        return source;
    }

    public ExprRefs refs() {
        return refs;
    }

    public @Nullable DataValue value(ScriptContext context) {
        return root.eval(context);
    }

    public DataValue valueOrZero(ScriptContext context) {
        DataValue value = root.eval(context);
        return value == null ? Expr.ZERO : value;
    }

    public boolean test(ScriptContext context) {
        return Expr.truthy(root.eval(context));
    }

    public double number(ScriptContext context) {
        return Expr.number(root.eval(context));
    }

    public String display(ScriptContext context) {
        return Expr.display(root.eval(context));
    }

    @Override
    public String toString() {
        return source;
    }
}
