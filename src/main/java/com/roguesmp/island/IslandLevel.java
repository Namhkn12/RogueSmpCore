package com.roguesmp.island;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public record IslandLevel(int maxEnergyNetworks, int energyNetworkCapacity, Map<String, Integer> blockLimits, List<Requirement> requirements, List<Cost> costs) {

    public static final IslandLevel DEFAULT = new IslandLevel(1, 10000, Map.of(), List.of(), List.of());

    public static final Codec<IslandLevel> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max_energy_networks", DEFAULT.maxEnergyNetworks()).forGetter(IslandLevel::maxEnergyNetworks),
            Codec.INT.optionalFieldOf("energy_network_capacity", DEFAULT.energyNetworkCapacity()).forGetter(IslandLevel::energyNetworkCapacity),
            Codec.unboundedMap(Codec.INT).optionalFieldOf("block_limits", Map.of()).forGetter(IslandLevel::blockLimits),
            Codec.listOf(Requirement.CODEC).optionalFieldOf("requirements", List.of()).forGetter(IslandLevel::requirements),
            Codec.listOf(Cost.CODEC).optionalFieldOf("costs", List.of()).forGetter(IslandLevel::costs),
            IslandLevel::new
    );

    public static IslandLevel of(int level) {
        Map.Entry<Integer, IslandLevel> entry = byLevel().floorEntry(level);
        return entry == null ? DEFAULT : entry.getValue();
    }

    public static @Nullable Map.Entry<Integer, IslandLevel> next(int level) {
        return byLevel().higherEntry(level);
    }

    private static NavigableMap<Integer, IslandLevel> byLevel() {
        NavigableMap<Integer, IslandLevel> levels = new TreeMap<>();
        for (Map.Entry<String, IslandLevel> entry : Registries.ISLAND_LEVEL.getAll().entrySet()) {
            try {
                levels.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {
            }
        }
        return levels;
    }
}
