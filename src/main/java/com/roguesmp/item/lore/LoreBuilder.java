package com.roguesmp.item.lore;

import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LoreBuilder {

    private final Map<Integer, List<Component>> lines = new TreeMap<>();

    public void putLines(int position, List<Component> lines) {
        this.lines.put(position, lines);
    }

    public List<Component> build() {
        List<Component> result = new ArrayList<>();
        for (List<Component> lineList : lines.values()) {
            result.addAll(lineList);
        }
        return result;
    }
}
