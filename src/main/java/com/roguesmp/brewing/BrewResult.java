package com.roguesmp.brewing;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.effect.SmpEffect;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BrewResult(Map<BrewStat, Double> stats, int durationTicks, int color) {

    public BrewResult {
        stats = new LinkedHashMap<>(stats);
    }

    public Color bukkitColor() {
        return Color.fromRGB(color & 0xFFFFFF);
    }

    public Map<Attributes, Double> attributes() {
        Map<Attributes, Double> attributes = new EnumMap<>(Attributes.class);
        stats.forEach((stat, value) -> {
            if (stat instanceof BrewStat.AttributeStat(Attributes attribute)) attributes.put(attribute, value);
        });
        return attributes;
    }

    public List<SmpEffect> effects() {
        List<SmpEffect> effects = new ArrayList<>();
        stats.forEach((stat, value) -> {
            SmpEffect effect = stat.createEffect(value, durationTicks);
            if (effect != null) effects.add(effect);
        });
        return effects;
    }
}
