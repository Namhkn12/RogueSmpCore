package com.roguesmp.dungeon.script.expr;

import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Locale;

final class ExprParser {

    enum Scope { VARIABLE, PLAYER_VARIABLE, TEMP, CONTEXT, QUERY, MATH }

    record Name(Scope scope, String name) {
    }

    static final class ParseException extends RuntimeException {
        ParseException(String message, int position) {
            super(message + " at " + position);
        }
    }


    private enum Kind { NUMBER, STRING, IDENT, SYMBOL, END }

    private record Token(Kind kind, String text, int position) {
        boolean is(String symbol) {
            return kind == Kind.SYMBOL && text.equals(symbol);
        }
    }

    private static final Set<String> TWO_CHAR = Set.of("==", "!=", "<=", ">=", "&&", "||", "??", "+=", "-=", "*=", "/=");
    private static final String ONE_CHAR = "+-*/%<>!?:(),;=";


    private static List<Token> lex(String source) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < source.length() && Character.isDigit(source.charAt(i + 1)))) {
                int start = i;
                while (i < source.length() && (Character.isDigit(source.charAt(i)) || source.charAt(i) == '.')) i++;
                tokens.add(new Token(Kind.NUMBER, source.substring(start, i), start));
            } else if (c == '\'') {
                int end = source.indexOf('\'', i + 1);
                if (end < 0) throw new ParseException("unclosed string", i);
                tokens.add(new Token(Kind.STRING, source.substring(i + 1, end), i));
                i = end + 1;
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < source.length() && isIdentifierPart(source.charAt(i))) i++;
                if (source.charAt(i - 1) == '.') throw new ParseException("name ends with '.'", start);
                tokens.add(new Token(Kind.IDENT, source.substring(start, i), start));
            } else if (i + 1 < source.length() && TWO_CHAR.contains(source.substring(i, i + 2))) {
                tokens.add(new Token(Kind.SYMBOL, source.substring(i, i + 2), i));
                i += 2;
            } else if (ONE_CHAR.indexOf(c) >= 0) {
                tokens.add(new Token(Kind.SYMBOL, String.valueOf(c), i));
                i++;
            } else {
                throw new ParseException("unexpected '" + c + "'", i);
            }
        }
        tokens.add(new Token(Kind.END, "", source.length()));
        return tokens;
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private final List<Token> tokens;
    private final ExprRefs.Builder refs = new ExprRefs.Builder();
    private int index;

    private ExprParser(String source) {
        this.tokens = lex(source);
    }

    static Parsed<Expr> expression(String source) {
        ExprParser parser = new ExprParser(source);
        Expr expr = parser.ternary();
        parser.expectEnd();
        return new Parsed<>(expr, parser.refs.build());
    }

    static Parsed<List<Statements.Assignment>> statements(String source) {
        ExprParser parser = new ExprParser(source);
        List<Statements.Assignment> assignments = new ArrayList<>();
        do {
            if (parser.peek().kind() == Kind.END) break;
            assignments.add(parser.assignment());
        } while (parser.accept(";"));
        parser.expectEnd();
        if (assignments.isEmpty()) throw new ParseException("expected an assignment", 0);
        return new Parsed<>(assignments, parser.refs.build());
    }

    record Parsed<T>(T value, ExprRefs refs) {
    }

    static @Nullable Name resolve(String identifier) {
        int dot = identifier.indexOf('.');
        if (dot < 0) return null;
        String prefix = identifier.substring(0, dot).toLowerCase(Locale.ROOT);
        String name = identifier.substring(dot + 1);
        Scope scope = switch (prefix) {
            case "v", "variable", "var" -> Scope.VARIABLE;
            case "p", "pvar" -> Scope.PLAYER_VARIABLE;
            case "t", "temp" -> Scope.TEMP;
            case "c", "context" -> Scope.CONTEXT;
            case "q", "query" -> Scope.QUERY;
            case "math" -> Scope.MATH;
            default -> null;
        };
        if (scope == null) return null;
        if (scope == Scope.QUERY || scope == Scope.MATH) name = name.toLowerCase(Locale.ROOT);
        return new Name(scope, name);
    }

    private Statements.Assignment assignment() {
        Token target = next();
        Name name = target.kind() == Kind.IDENT ? resolve(target.text()) : null;
        if (name == null || !(name.scope() == Scope.VARIABLE || name.scope() == Scope.PLAYER_VARIABLE || name.scope() == Scope.TEMP)) {
            throw new ParseException("expected v., p. or t. variable to assign", target.position());
        }
        Token operator = next();
        Statements.Operator op = switch (operator.text()) {
            case "=" -> Statements.Operator.SET;
            case "+=" -> Statements.Operator.ADD;
            case "-=" -> Statements.Operator.SUBTRACT;
            case "*=" -> Statements.Operator.MULTIPLY;
            case "/=" -> Statements.Operator.DIVIDE;
            default -> throw new ParseException("expected =, +=, -=, *= or /=", operator.position());
        };
        if (op != Statements.Operator.SET && name.scope() != Scope.TEMP) refs.var(name.name(), name.scope() == Scope.PLAYER_VARIABLE);
        if (name.scope() == Scope.PLAYER_VARIABLE) refs.needsPlayer();
        return new Statements.Assignment(name.scope(), name.name(), op, ternary());
    }

    private Expr ternary() {
        Expr test = coalesce();
        if (!accept("?")) return test;
        Expr then = ternary();
        expect(":");
        return new Expr.Ternary(test, then, ternary());
    }

    private Expr coalesce() {
        Expr left = or();
        while (accept("??")) left = new Expr.Coalesce(left, or());
        return left;
    }

    private Expr or() {
        Expr left = and();
        while (accept("||")) left = new Expr.Or(left, and());
        return left;
    }

    private Expr and() {
        Expr left = equality();
        while (accept("&&")) left = new Expr.And(left, equality());
        return left;
    }

    private Expr equality() {
        Expr left = comparison();
        while (true) {
            if (accept("==")) left = new Expr.Equals(left, comparison(), false);
            else if (accept("!=")) left = new Expr.Equals(left, comparison(), true);
            else return left;
        }
    }

    private Expr comparison() {
        Expr left = additive();
        while (true) {
            Expr.Compare compare = null;
            for (Expr.Compare candidate : Expr.Compare.values()) {
                if (peek().is(candidate.symbol)) compare = candidate;
            }
            if (compare == null) return left;
            index++;
            left = new Expr.Comparison(compare, left, additive());
        }
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (true) {
            if (accept("+")) left = new Expr.Add(left, multiplicative());
            else if (accept("-")) left = new Expr.Arithmetic((a, b) -> a - b, left, multiplicative());
            else return left;
        }
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (true) {
            if (accept("*")) left = new Expr.Arithmetic((a, b) -> a * b, left, unary());
            else if (accept("/")) left = new Expr.Arithmetic((a, b) -> a / b, left, unary());
            else if (accept("%")) left = new Expr.Arithmetic((a, b) -> a % b, left, unary());
            else return left;
        }
    }

    private Expr unary() {
        if (accept("!")) return new Expr.Not(unary());
        if (accept("-")) return new Expr.Negate(unary());
        return primary();
    }

    private Expr primary() {
        Token token = next();
        switch (token.kind()) {
            case NUMBER -> {
                try {
                    return new Expr.Constant(DataValue.of(Double.parseDouble(token.text())));
                } catch (NumberFormatException e) {
                    throw new ParseException("bad number '" + token.text() + "'", token.position());
                }
            }
            case STRING -> {
                return new Expr.Constant(DataValue.of(token.text()));
            }
            case IDENT -> {
                return identifier(token);
            }
            default -> {
                if (token.is("(")) {
                    Expr inner = ternary();
                    expect(")");
                    return inner;
                }
                throw new ParseException(token.kind() == Kind.END ? "unexpected end" : "unexpected '" + token.text() + "'", token.position());
            }
        }
    }

    private Expr identifier(Token token) {
        String lower = token.text().toLowerCase(Locale.ROOT);
        if (lower.equals("true")) return new Expr.Constant(Expr.TRUE);
        if (lower.equals("false")) return new Expr.Constant(Expr.FALSE);

        Name name = resolve(token.text());
        if (name == null) throw new ParseException("unknown name '" + token.text() + "' (use v., p., t., c., q. or math.)", token.position());
        return switch (name.scope()) {
            case VARIABLE, PLAYER_VARIABLE -> {
                boolean perPlayer = name.scope() == Scope.PLAYER_VARIABLE;
                refs.var(name.name(), perPlayer);
                yield new Expr.Variable(name.name(), perPlayer);
            }
            case TEMP -> new Expr.Temp(name.name());
            case CONTEXT -> {
                refs.context(name.name());
                yield new Expr.Context(name.name());
            }
            case QUERY -> {
                ExprFunction query = Registries.DUNGEON_QUERY.get(name.name());
                if (query == null) throw new ParseException("unknown query 'q." + name.name() + "'", token.position());
                refs.query(name.name(), query);
                yield call("q." + name.name(), query, token, false);
            }
            case MATH -> {
                ExprFunction function = ExprFunction.math(name.name());
                if (function == null) throw new ParseException("unknown function 'math." + name.name() + "'", token.position());
                yield call("math." + name.name(), function, token, true);
            }
        };
    }

    private Expr call(String display, ExprFunction function, Token token, boolean needsParentheses) {
        List<Expr> args = new ArrayList<>();
        if (accept("(")) {
            if (!accept(")")) {
                do args.add(ternary()); while (accept(","));
                expect(")");
            }
        } else if (needsParentheses) {
            throw new ParseException(display + " needs ()", token.position());
        }
        if (args.size() < function.minArgs() || args.size() > function.maxArgs()) {
            String expected = function.minArgs() == function.maxArgs() ? String.valueOf(function.minArgs()) : function.minArgs() + "-" + function.maxArgs();
            throw new ParseException(display + " takes " + expected + " argument(s), got " + args.size(), token.position());
        }
        return new Expr.Call(function, List.copyOf(args));
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token next() {
        Token token = tokens.get(index);
        if (token.kind() != Kind.END) index++;
        return token;
    }

    private boolean accept(String symbol) {
        if (!peek().is(symbol)) return false;
        index++;
        return true;
    }

    private void expect(String symbol) {
        Token token = peek();
        if (!accept(symbol)) throw new ParseException("expected '" + symbol + "'", token.position());
    }

    private void expectEnd() {
        Token token = peek();
        if (token.kind() != Kind.END) throw new ParseException("unexpected '" + token.text() + "'", token.position());
    }
}
