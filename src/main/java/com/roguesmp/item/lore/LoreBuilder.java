package com.roguesmp.item.lore;

import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LoreBuilder {

    private final Map<Integer, List<Component>> lines = new HashMap<>();

    public void putLines(int position, List<Component> lines) {
        this.lines.put(position, lines);
    }

    public List<Component> build() {
        return lines.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

}
