package com.roguesmp.island;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

import java.util.Map;

public record IslandLevel(int maxEnergyNetworks, int energyNetworkCapacity) {

    public static final IslandLevel DEFAULT = new IslandLevel(1, 10000);

    public static final Codec<IslandLevel> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max_energy_networks", DEFAULT.maxEnergyNetworks()).forGetter(IslandLevel::maxEnergyNetworks),
            Codec.INT.optionalFieldOf("energy_network_capacity", DEFAULT.energyNetworkCapacity()).forGetter(IslandLevel::energyNetworkCapacity),
            IslandLevel::new
    );

    public static IslandLevel of(int level) {
        IslandLevel best = DEFAULT;
        int bestLevel = Integer.MIN_VALUE;
        for (Map.Entry<String, IslandLevel> entry : Registries.ISLAND_LEVEL.getAll().entrySet()) {
            int configured;
            try {
                configured = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            if (configured <= level && configured > bestLevel) {
                bestLevel = configured;
                best = entry.getValue();
            }
        }
        return best;
    }
}
