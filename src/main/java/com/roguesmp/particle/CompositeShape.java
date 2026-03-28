package com.roguesmp.particle;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CompositeShape implements ParticleShape {
    private final List<ParticleShape> parts = new ArrayList<>();

    public CompositeShape add(ParticleShape part) {
        parts.add(part);
        return this;
    }

    @Override
    public List<Vector> getOffsets(int tick) {
        List<Vector> list = new ArrayList<>();
        for (ParticleShape s : parts) {
            list.addAll(s.getOffsets(tick));
        }
        return list;
    }
}
