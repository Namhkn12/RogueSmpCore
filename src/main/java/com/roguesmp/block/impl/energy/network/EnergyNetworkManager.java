package com.roguesmp.block.impl.energy.network;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.impl.energy.WirelessNodeBlock;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnergyNetworkManager {

    private static EnergyNetworkManager INSTANCE;

    private final Map<EnergyNetworkData, EnergyNetwork> active = new IdentityHashMap<>();

    private EnergyNetworkManager(RogueSmpCore plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        for (EnergyNetwork network : List.copyOf(active.values())) network.tick();
    }

    public EnergyNetwork join(WirelessNodeBlock node, IslandData island, EnergyNetworkData data) {
        EnergyNetwork network = active.computeIfAbsent(data, key -> new EnergyNetwork(island, key));
        network.addNode(node);
        return network;
    }

    public void leave(WirelessNodeBlock node, EnergyNetwork network) {
        network.removeNode(node);
        if (network.isEmpty()) active.remove(network.data());
    }

    public @Nullable EnergyNetwork getActive(EnergyNetworkData data) {
        return active.get(data);
    }

    public static boolean canCreate(IslandData island) {
        return island.getEnergyNetworks().size() < IslandLevel.of(island.getLevel()).maxEnergyNetworks();
    }

    public @Nullable EnergyNetworkData create(IslandData island) {
        if (!canCreate(island)) return null;

        String id;
        do id = UUID.randomUUID().toString().substring(0, 8);
        while (island.getEnergyNetwork(id) != null);

        EnergyNetworkData data = new EnergyNetworkData(id, "Mạng #" + (island.getEnergyNetworks().size() + 1), 0);
        island.addEnergyNetwork(data);
        return data;
    }

    public void delete(IslandData island, EnergyNetworkData data) {
        EnergyNetwork network = active.remove(data);
        if (network != null) List.copyOf(network.nodes()).forEach(WirelessNodeBlock::disconnect);
        island.removeEnergyNetwork(data);
    }

    public static EnergyNetworkManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(EnergyNetworkManager.class.getSimpleName() + " is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new EnergyNetworkManager(plugin);
    }
}
