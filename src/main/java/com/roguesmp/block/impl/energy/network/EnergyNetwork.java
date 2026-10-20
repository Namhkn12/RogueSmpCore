package com.roguesmp.block.impl.energy.network;

import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.impl.energy.WirelessNodeBlock;
import com.roguesmp.block.transfer.EnergyTransfer;
import com.roguesmp.block.transfer.SideConfig;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EnergyNetwork implements EnergyStorage {

    private static final int SPARK_INTERVAL = 10;

    private final IslandData island;
    private final EnergyNetworkData data;
    private final Set<WirelessNodeBlock> nodes = new LinkedHashSet<>();
    private int lastEnergy;
    private int lastFlow;
    private int sparkTicks;

    EnergyNetwork(IslandData island, EnergyNetworkData data) {
        this.island = island;
        this.data = data;
        this.lastEnergy = data.energy();
    }

    void tick() {
        lastFlow = EnergyTransfer.split(this, collectConsumers(), throughput());
        if (data.energy() != lastEnergy) {
            lastEnergy = data.energy();
            island.setDirty(true);
        }
        if (++sparkTicks >= SPARK_INTERVAL) {
            sparkTicks = 0;
            if (lastFlow > 0) spark();
        }
    }

    private List<EnergyStorage> collectConsumers() {
        Set<EnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<EnergyStorage> consumers = new ArrayList<>();
        for (WirelessNodeBlock node : nodes) {
            Block origin = node.getLocation().getBlock();
            for (BlockFace face : SideConfig.FACES) {
                EnergyStorage receiver = EnergyTransfer.findReceiver(origin.getRelative(face));
                if (receiver == null || receiver instanceof WirelessNodeBlock) continue;
                if (receiver.getEnergy() >= receiver.getMaxEnergy() || !seen.add(receiver)) continue;
                consumers.add(receiver);
            }
        }
        return consumers;
    }

    private void spark() {
        for (WirelessNodeBlock node : nodes) {
            Location top = node.getLocation().toCenterLocation().add(0, 0.6, 0);
            top.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, top, 3, 0.2, 0.1, 0.2, 0);
        }
    }

    void addNode(WirelessNodeBlock node) {
        nodes.add(node);
    }

    void removeNode(WirelessNodeBlock node) {
        nodes.remove(node);
    }

    boolean isEmpty() {
        return nodes.isEmpty();
    }

    public Set<WirelessNodeBlock> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public EnergyNetworkData data() {
        return data;
    }

    public int throughput() {
        int throughput = 0;
        for (WirelessNodeBlock node : nodes) throughput += node.throughput();
        return throughput;
    }

    public int lastFlow() {
        return lastFlow;
    }

    @Override
    public int getEnergy() {
        return data.energy();
    }

    @Override
    public int getMaxEnergy() {
        return IslandLevel.of(island.getLevel()).energyNetworkCapacity();
    }

    @Override
    public int addEnergy(int amount) {
        int accepted = Math.max(0, Math.min(amount, getMaxEnergy() - data.energy()));
        data.setEnergy(data.energy() + accepted);
        return accepted;
    }

    @Override
    public int removeEnergy(int amount) {
        int removed = Math.max(0, Math.min(amount, data.energy()));
        data.setEnergy(data.energy() - removed);
        return removed;
    }
}
