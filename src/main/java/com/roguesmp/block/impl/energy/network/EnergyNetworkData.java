package com.roguesmp.block.impl.energy.network;

import com.roguesmp.codec.Codec;

public class EnergyNetworkData {

    public static final Codec<EnergyNetworkData> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(EnergyNetworkData::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(EnergyNetworkData::name),
            Codec.INT.optionalFieldOf("energy", 0).forGetter(EnergyNetworkData::energy),
            EnergyNetworkData::new
    );

    private final String id;
    private final String name;
    private int energy;

    public EnergyNetworkData(String id, String name, int energy) {
        this.id = id;
        this.name = name;
        this.energy = energy;
    }

    public EnergyNetworkData copy() {
        return new EnergyNetworkData(id, name, energy);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int energy() {
        return energy;
    }

    void setEnergy(int energy) {
        this.energy = energy;
    }
}
