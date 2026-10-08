package com.roguesmp.fishing;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;

import java.util.List;
import java.util.Map;

public record FishingHotspot(
        String displayName,
        int weight,
        int lifetimeSeconds,
        double radius,
        Map<String, Double> tierWeightMultipliers,
        int barSegmentsBonus,
        double barTurnChanceMultiplier,
        double luckBonus,
        LootMode lootMode,
        List<Holder<LootTable>> lootTables
) {

    public enum LootMode { EXTRA, REPLACE }

    public static final Codec<FishingHotspot> CODEC = Codec.composite(
            Codec.STRING.fieldOf("display_name").forGetter(FishingHotspot::displayName),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(FishingHotspot::weight),
            Codec.INT.fieldOf("lifetime_seconds").forGetter(FishingHotspot::lifetimeSeconds),
            Codec.DOUBLE.optionalFieldOf("radius", 6.0).forGetter(FishingHotspot::radius),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("tier_weight_multipliers", Map.of()).forGetter(FishingHotspot::tierWeightMultipliers),
            Codec.INT.optionalFieldOf("bar_segments_bonus", 0).forGetter(FishingHotspot::barSegmentsBonus),
            Codec.DOUBLE.optionalFieldOf("bar_turn_chance_multiplier", 1.0).forGetter(FishingHotspot::barTurnChanceMultiplier),
            Codec.DOUBLE.optionalFieldOf("luck_bonus", 0.0).forGetter(FishingHotspot::luckBonus),
            Codec.enumOf(LootMode.class).optionalFieldOf("loot_mode", LootMode.EXTRA).forGetter(FishingHotspot::lootMode),
            Codec.listOf(Registry.referenceCodec(() -> Registries.LOOT_TABLE)).optionalFieldOf("loot_tables", List.of()).forGetter(FishingHotspot::lootTables),
            FishingHotspot::new
    );

    public double tierMultiplier(String tierId) {
        return tierWeightMultipliers.getOrDefault(tierId, 1.0);
    }
}
