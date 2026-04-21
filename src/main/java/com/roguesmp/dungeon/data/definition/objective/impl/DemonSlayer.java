package com.roguesmp.dungeon.data.definition.objective.impl;

import com.roguesmp.dungeon.data.definition.objective.CompletionScope;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemonSlayer extends MonsterHunter {

    public static final String TYPE = "demon_slayer";

    public DemonSlayer() {
        setRequire(1);
    }

    @Override
    public CompletionScope getCompletionScope() {
        return CompletionScope.DUNGEON;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        return data;
    }

    @Override
    public List<String> getScoreBoardLine() {
        String bossName = getTargetId() != null ? getTargetId() : "Boss";
        String line = isCompleted()
                ? "§a✔ Slay " + bossName
                : "§7Slay §c" + bossName;
        return List.of(line);
    }
}
