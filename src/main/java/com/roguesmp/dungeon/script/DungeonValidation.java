package com.roguesmp.dungeon.script;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.action.DungeonActions;
import com.roguesmp.registry.Registries;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

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

    private DungeonValidation(String path, Report report) {
        this.path = path;
        this.report = report;
    }

    public static void validateAll() {
        int warnings = 0;
        for (Map.Entry<String, DungeonDefinition> entry : Registries.DUNGEON.getAll().entrySet()) {
            Report report = new Report();
            DungeonValidation root = new DungeonValidation("", report);
            entry.getValue().variables().keySet().forEach(report.writtenVars::add);
            entry.getValue().features().forEach((featureId, feature) -> feature.validate(featureId, root.at("features." + featureId)));
            report.finish();
            report.warnings.forEach(warning -> RogueSmpCore.LOGGER.warn("Dungeon {}: {}", entry.getKey(), warning));
            warnings += report.warnings.size();
        }
        if (warnings > 0) RogueSmpCore.LOGGER.warn("Dungeon validation found {} problem(s)", warnings);
    }

    public DungeonValidation at(String segment) {
        return new DungeonValidation(path.isEmpty() ? segment : segment.startsWith("[") ? path + segment : path + "." + segment, report);
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
    }

    public void checkMob(String mob) {
        if (Registries.ENTITY.get(mob) != null) return;
        NamespacedKey key = NamespacedKey.fromString(mob);
        if (key == null || org.bukkit.Registry.ENTITY_TYPE.get(key) == null) warn("unknown mob '" + mob + "'");
    }

    public void checkItem(String item) {
        if (Registries.ITEM.get(item) == null && Material.matchMaterial(item) == null) warn("unknown item '" + item + "'");
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

        private void finish() {
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
    }
}
