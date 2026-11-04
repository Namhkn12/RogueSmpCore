package com.roguesmp.dungeon.script.expr;

import java.util.HashSet;
import java.util.Set;

public record ExprRefs(Set<String> vars, Set<String> playerVars, Set<String> context, Set<String> queries, boolean needsPlayer) {

    public static final ExprRefs EMPTY = new ExprRefs(Set.of(), Set.of(), Set.of(), Set.of(), false);

    public ExprRefs merge(ExprRefs other) {
        return new ExprRefs(union(vars, other.vars), union(playerVars, other.playerVars), union(context, other.context),
                union(queries, other.queries), needsPlayer || other.needsPlayer);
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.addAll(b);
        return Set.copyOf(result);
    }

    static final class Builder {
        private final Set<String> vars = new HashSet<>();
        private final Set<String> playerVars = new HashSet<>();
        private final Set<String> context = new HashSet<>();
        private final Set<String> queries = new HashSet<>();
        private boolean needsPlayer;

        void var(String name, boolean perPlayer) {
            (perPlayer ? playerVars : vars).add(name);
            if (perPlayer) needsPlayer = true;
        }

        void needsPlayer() {
            needsPlayer = true;
        }

        void context(String key) {
            context.add(key);
        }

        void query(String name, ExprFunction function) {
            queries.add(name);
            if (function.needsPlayer()) needsPlayer = true;
        }

        ExprRefs build() {
            return new ExprRefs(Set.copyOf(vars), Set.copyOf(playerVars), Set.copyOf(context), Set.copyOf(queries), needsPlayer);
        }
    }
}
