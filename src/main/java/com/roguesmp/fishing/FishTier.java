package com.roguesmp.fishing;

import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

public enum FishTier {

    COMMON(500, 12, 0.02, TextColor.color(0xC8C8C8)),
    UNCOMMON(500, 11, 0.03, TextColor.color(0x55FF55)),
    RARE(500, 10, 0.045, TextColor.color(0x55AAFF)),
    EPIC(500, 8, 0.055, TextColor.color(0xAA55FF)),
    LEGENDARY(500, 7, 0.055, TextColor.color(0xFFAA00));

    private static final String LOOT_TABLE_FOLDER = "fishing/";

    private final double weight;
    private final int barSegments;
    private final double barTurnChance;
    private final TextColor color;
    private final String id;

    FishTier(double weight, int barSegments, double barTurnChance, TextColor color) {
        this.weight = weight;
        this.barSegments = barSegments;
        this.barTurnChance = barTurnChance;
        this.color = color;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public double weight() {
        return weight;
    }

    public int barSegments() {
        return barSegments;
    }

    public double barTurnChance() {
        return barTurnChance;
    }

    public TextColor color() {
        return color;
    }

    public String id() {
        return id;
    }

    public Holder<LootTable> lootTable() {
        return Registries.LOOT_TABLE.getHolder(LOOT_TABLE_FOLDER + id);
    }
}
