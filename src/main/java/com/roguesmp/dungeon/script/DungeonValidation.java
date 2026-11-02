package com.roguesmp.dungeon.script;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.action.DungeonActions;
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
import java.util.regex.Matcher;

public final class DungeonValidation {

    private final String path;
    private final Report report;
    private final @Nullable String function;

    private DungeonValidation(String path, Report report, @Nullable String function) {
        this.path = path;
        this.report = report;
        this.function = function;
    }

    public static void validateAll() {
        int warnings = 0;
        for (Map.Entry<String, DungeonDefinition> entry : Registries.DUNGEON.getAll().entrySet()) {
            Report report = new Report();
            DungeonValidation root = new DungeonValidation("", report, null);
            entry.getValue().variables().keySet().forEach(report.writtenVars::add);
            report.functions.addAll(entry.getValue().functions().keySet());
            entry.getValue().functions().forEach((name, actions) -> {
                DungeonValidation at = new DungeonValidation("functions." + name, report, name);
                for (int i = 0; i < actions.size(); i++) actions.get(i).validate(at.at("[" + i + "]"));
            });
            entry.getValue().features().forEach((featureId, feature) -> feature.validate(featureId, root.at("features." + featureId)));
            report.finish();
            report.warnings.forEach(warning -> RogueSmpCore.LOGGER.warn("Dungeon {}: {}", entry.getKey(), warning));
            warnings += report.warnings.size();
        }
        if (warnings > 0) RogueSmpCore.LOGGER.warn("Dungeon validation found {} problem(s)", warnings);
    }

    public DungeonValidation at(String segment) {
        return new DungeonValidation(path.isEmpty() ? segment : segment.startsWith("[") ? path + segment : path + "." + segment, report, function);
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

    public void readsText(String text) {
        Matcher matcher = DungeonActions.VAR_PLACEHOLDER.matcher(text);
        while (matcher.find()) readsVar(matcher.group(1));
        Matcher playerMatcher = DungeonActions.PLAYER_VAR_PLACEHOLDER.matcher(text);
        while (playerMatcher.find()) readsVar(playerMatcher.group(1));
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

    public void checkFunction(String function) {
        if (!report.functions.contains(function)) warn("unknown function '" + function + "'");
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

    private static final class Report {
        private final List<String> warnings = new ArrayList<>();
        private final Map<String, Set<String>> emitted = new HashMap<>(DungeonEvent.BUILT_IN);
        private final List<Listen> listened = new ArrayList<>();
        private final Set<String> writtenVars = new HashSet<>();
        private final List<Listen> readVars = new ArrayList<>();
        private final Set<String> functions = new HashSet<>();
        private final Map<String, Set<String>> calls = new HashMap<>();

        private void finish() {
            for (String function : functions) {
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
            for (Listen read : readVars) {
                if (!writtenVars.contains((String) read.target())) {
                    warnings.add(read.path() + ": variable '" + read.target() + "' is never declared or set");
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
