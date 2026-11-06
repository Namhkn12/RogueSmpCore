package com.roguesmp.dungeon.script;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.action.DungeonAction;
import com.roguesmp.dungeon.listener.BukkitEventAdapters;
import com.roguesmp.dungeon.script.expr.ExprRefs;
import com.roguesmp.dungeon.script.expr.ScriptText;
import com.roguesmp.dungeon.script.expr.ScriptValue;
import com.roguesmp.dungeon.script.expr.Statements;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.ItemRef;
import org.bukkit.NamespacedKey;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DungeonValidation {

    private static final Set<String> ALWAYS_IN_CONTEXT = Set.of("player", "subject", "event");

    private enum ScopeKind { ANY, EVENT, NONE, PARAMS }

    private record Scope(ScopeKind kind, String event, Set<String> params) {
        static final Scope ANY = new Scope(ScopeKind.ANY, "", Set.of());
    }

    private final String path;
    private final Report report;
    private final @Nullable String function;
    private final Scope scope;

    private DungeonValidation(String path, Report report, @Nullable String function, Scope scope) {
        this.path = path;
        this.report = report;
        this.function = function;
        this.scope = scope;
    }

    public static void validateAll() {
        int warnings = 0;
        Set<String> usedEvents = new HashSet<>();
        for (Map.Entry<String, DungeonDefinition> entry : Registries.DUNGEON.getAll().entrySet()) {
            Report report = new Report();
            DungeonValidation root = new DungeonValidation("", report, null, Scope.ANY);
            entry.getValue().variables().keySet().forEach(report.writtenVars::add);
            entry.getValue().functions().forEach((name, function) -> report.functions.put(name, Set.copyOf(function.params())));
            entry.getValue().functions().forEach((name, function) -> {
                Scope scope = function.params().isEmpty() ? Scope.ANY : new Scope(ScopeKind.PARAMS, "", Set.copyOf(function.params()));
                DungeonAction.validateAll(function.actions(), new DungeonValidation("functions." + name, report, name, scope));
            });
            entry.getValue().features().forEach((featureId, feature) -> feature.validate(featureId, root.at("features." + featureId)));
            report.finish();
            report.warnings.forEach(warning -> RogueSmpCore.LOGGER.warn("Dungeon {}: {}", entry.getKey(), warning));
            warnings += report.warnings.size();
            report.listened.forEach(listen -> usedEvents.add(((EventMatcher) listen.target()).event()));
        }
        BukkitEventAdapters.listen(usedEvents);
        if (warnings > 0) RogueSmpCore.LOGGER.warn("Dungeon validation found {} problem(s)", warnings);
    }

    public DungeonValidation at(String segment) {
        return new DungeonValidation(path.isEmpty() ? segment : segment.startsWith("[") ? path + segment : path + "." + segment, report, function, scope);
    }

    public DungeonValidation inEvent(String event) {
        return new DungeonValidation(path, report, function, new Scope(ScopeKind.EVENT, event, Set.of()));
    }

    public DungeonValidation withoutEvent() {
        return new DungeonValidation(path, report, function, new Scope(ScopeKind.NONE, "", Set.of()));
    }

    public void warn(String message) {
        report.warnings.add(path + ": " + message);
    }

    public void emits(String event, Set<String> dataKeys) {
        report.emitted.computeIfAbsent(event, id -> new HashSet<>()).addAll(dataKeys);
    }

    public void listens(EventMatcher matcher) {
        report.listened.add(new Listen(path, matcher));
    }

    public void writesVar(String name) {
        report.writtenVars.add(name);
    }

    public void readsVar(String name) {
        report.readVars.add(new Listen(path, name));
    }

    public void reads(ExprRefs refs) {
        refs.vars().forEach(this::readsVar);
        refs.playerVars().forEach(this::readsVar);
        if (!refs.context().isEmpty() || refs.needsPlayer()) report.contextReads.add(new Listen(path, new ContextRead(refs, scope)));
    }

    public void reads(ScriptValue value) {
        reads(value.refs());
        String error = value.suspiciousText();
        if (error != null) warn("looks like an expression but is used as plain text: " + error);
    }

    public void reads(ScriptText text) {
        reads(text.refs());
    }

    public void runs(Statements statements) {
        reads(statements.reads());
        statements.writes().forEach(this::writesVar);
    }

    public void cancels() {
        report.cancels.add(new Listen(path, scope));
    }

    public void checkMob(String mob) {
        if (Registries.ENTITY.get(mob) != null) return;
        NamespacedKey key = NamespacedKey.fromString(mob);
        if (key == null || org.bukkit.Registry.ENTITY_TYPE.get(key) == null) warn("unknown mob '" + mob + "'");
    }

    public void checkItem(String item) {
        boolean known = ItemRef.isVanilla(item) ? ItemRef.vanillaMaterial(item) != null : Registries.ITEM.get(item) != null;
        if (!known) warn("unknown item '" + item + "' (vanilla items need the minecraft: prefix)");
    }

    public void checkFunction(String function, Set<String> args) {
        Set<String> params = report.functions.get(function);
        if (params == null) {
            warn("unknown function '" + function + "'");
        } else {
            params.stream().filter(param -> !args.contains(param)).forEach(param -> warn("missing argument '" + param + "' for function '" + function + "'"));
            args.stream().filter(arg -> !params.contains(arg)).forEach(arg -> warn("function '" + function + "' has no parameter '" + arg + "'"));
        }
        if (this.function != null) report.calls.computeIfAbsent(this.function, caller -> new HashSet<>()).add(function);
    }

    public void checkRoom(String room) {
        if (Registries.DUNGEON_ROOM.get(room) == null) warn("unknown room '" + room + "'");
    }

    public void checkLootTable(String table) {
        if (Registries.LOOT_TABLE.get(table) == null) warn("unknown loot table '" + table + "'");
    }

    private record Listen(String path, Object target) {
    }

    private record ContextRead(ExprRefs refs, Scope scope) {
    }

    private static final class Report {
        private final List<String> warnings = new ArrayList<>();
        private final Map<String, Set<String>> emitted = new HashMap<>();
        private final List<Listen> listened = new ArrayList<>();
        private final Set<String> writtenVars = new HashSet<>();
        private final List<Listen> readVars = new ArrayList<>();
        private final List<Listen> contextReads = new ArrayList<>();
        private final List<Listen> cancels = new ArrayList<>();
        private final Map<String, Set<String>> functions = new HashMap<>();
        private final Map<String, Set<String>> calls = new HashMap<>();

        private Report() {
            DungeonEvent.BUILT_IN.forEach((event, keys) -> emitted.put(event, new HashSet<>(keys)));
            BukkitEventAdapters.adapters().forEach(adapter -> adapter.emits().forEach(emits -> emitted.computeIfAbsent(emits.id(), id -> new HashSet<>()).addAll(emits.dataKeys())));
        }

        private void finish() {
            for (String function : functions.keySet()) {
                if (callsItself(function, function, new HashSet<>())) warnings.add("functions." + function + ": calls itself (directly or through other functions)");
            }
            for (Listen listen : listened) {
                EventMatcher matcher = (EventMatcher) listen.target();
                Set<String> keys = emitted.get(matcher.event());
                if (keys == null) {
                    warnings.add(listen.path() + ": event '" + matcher.event() + "' is never fired by this dungeon");
                    continue;
                }
                for (String key : matcher.data().keySet()) {
                    if (!keys.contains(key)) warnings.add(listen.path() + ": event '" + matcher.event() + "' never carries data '" + key + "'");
                }
            }
            for (Listen read : contextReads) checkContext(read.path(), (ContextRead) read.target());
            for (Listen cancel : cancels) {
                Scope scope = (Scope) cancel.target();
                if (scope.kind() == ScopeKind.NONE || scope.kind() == ScopeKind.PARAMS) {
                    warnings.add(cancel.path() + ": 'cancel' needs a triggering event");
                } else if (scope.kind() == ScopeKind.EVENT && !BukkitEventAdapters.isCancellable(scope.event())) {
                    warnings.add(cancel.path() + ": event '" + scope.event() + "' can't be cancelled");
                }
            }
            for (Listen read : readVars) {
                if (!writtenVars.contains((String) read.target())) {
                    warnings.add(read.path() + ": variable '" + read.target() + "' is never declared or set");
                }
            }
        }

        private void checkContext(String path, ContextRead read) {
            Scope scope = read.scope();
            ExprRefs refs = read.refs();
            switch (scope.kind()) {
                case ANY -> {
                }
                case NONE -> {
                    refs.context().forEach(key -> warnings.add(path + ": c." + key + " is not available here (no triggering event)"));
                    if (refs.needsPlayer()) warnings.add(path + ": p. variables and player queries need a player, there is none here");
                }
                case PARAMS -> refs.context().stream().filter(key -> !scope.params().contains(key))
                        .forEach(key -> warnings.add(path + ": c." + key + " is not a parameter of this function " + scope.params()));
                case EVENT -> {
                    Set<String> keys = emitted.get(scope.event());
                    if (keys != null) {
                        refs.context().stream().filter(key -> !keys.contains(key) && !ALWAYS_IN_CONTEXT.contains(key))
                                .forEach(key -> warnings.add(path + ": c." + key + " is not provided by event '" + scope.event() + "' (has: " + String.join(", ", keys) + ")"));
                    }
                    if (refs.needsPlayer() && DungeonEvent.NO_PLAYER.contains(scope.event())) {
                        warnings.add(path + ": p. variables and player queries need a player, event '" + scope.event() + "' has none");
                    }
                }
            }
        }

        private boolean callsItself(String start, String current, Set<String> visited) {
            for (String next : calls.getOrDefault(current, Set.of())) {
                if (next.equals(start)) return true;
                if (visited.add(next) && callsItself(start, next, visited)) return true;
            }
            return false;
        }
    }
}
