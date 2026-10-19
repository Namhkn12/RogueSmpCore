package com.roguesmp.trade;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeLayout {

    public static final char OFFER = 'O';
    public static final char PREV_PAGE = '<';
    public static final char NEXT_PAGE = '>';

    public static final TradeLayout DEFAULT = new TradeLayout(List.of(
            "#########",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "#OOOOOOO#",
            "###<#>###"
    ), Map.of("#", Material.BLACK_STAINED_GLASS_PANE), Material.LIGHT_GRAY_STAINED_GLASS_PANE);

    public static final Codec<TradeLayout> CODEC = Codec.composite(
            Codec.listOf(Codec.STRING).fieldOf("pattern").forGetter(TradeLayout::getPattern),
            Codec.unboundedMap(Codec.enumOf(Material.class)).optionalFieldOf("keys", Map.of()).forGetter(TradeLayout::getRawKeys),
            Codec.enumOf(Material.class).optionalFieldOf("fill", Material.LIGHT_GRAY_STAINED_GLASS_PANE).forGetter(TradeLayout::getFill),
            TradeLayout::new
    ).comapFlatMap(TradeLayout::validate, layout -> layout);

    private final List<String> pattern;
    private final Map<String, Material> rawKeys;
    private final Map<Character, Material> keys = new HashMap<>();
    private final Material fill;
    private final List<Integer> offerSlots = new ArrayList<>();
    private int prevPageSlot = -1;
    private int nextPageSlot = -1;

    public TradeLayout(List<String> pattern, Map<String, Material> rawKeys, Material fill) {
        this.pattern = pattern;
        this.rawKeys = rawKeys;
        this.fill = fill;
        rawKeys.forEach((key, material) -> {
            if (key.length() == 1) keys.put(key.charAt(0), material);
        });
        for (int row = 0; row < pattern.size(); row++) {
            String line = pattern.get(row);
            for (int col = 0; col < line.length(); col++) {
                int slot = row * 9 + col;
                switch (line.charAt(col)) {
                    case OFFER -> offerSlots.add(slot);
                    case PREV_PAGE -> prevPageSlot = slot;
                    case NEXT_PAGE -> nextPageSlot = slot;
                    default -> {
                    }
                }
            }
        }
    }

    private static DataResult<TradeLayout> validate(TradeLayout layout) {
        List<String> pattern = layout.pattern;
        if (pattern.isEmpty() || pattern.size() > 6) return DataResult.error("Trade layout pattern must have 1-6 rows, got " + pattern.size());
        for (String line : pattern) {
            if (line.length() != 9) return DataResult.error("Trade layout row must be exactly 9 characters: '" + line + "'");
        }
        for (String key : layout.rawKeys.keySet()) {
            if (key.length() != 1) return DataResult.error("Trade layout key must be a single character: '" + key + "'");
        }
        if (layout.offerSlots.isEmpty()) return DataResult.error("Trade layout needs at least one '" + OFFER + "' slot");
        return DataResult.success(layout);
    }

    public int getRows() {
        return pattern.size();
    }

    public char charAt(int slot) {
        return pattern.get(slot / 9).charAt(slot % 9);
    }

    public List<String> getPattern() {
        return pattern;
    }

    public Map<Character, Material> getKeys() {
        return keys;
    }

    public Map<String, Material> getRawKeys() {
        return rawKeys;
    }

    public Material getFill() {
        return fill;
    }

    public List<Integer> getOfferSlots() {
        return offerSlots;
    }

    public int getPrevPageSlot() {
        return prevPageSlot;
    }

    public int getNextPageSlot() {
        return nextPageSlot;
    }
}
