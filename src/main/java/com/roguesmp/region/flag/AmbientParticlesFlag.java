package com.roguesmp.region.flag;

import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;

public final class AmbientParticlesFlag extends TextValueFlag<AmbientParticles> {

    private static final int DEFAULT_COUNT = 1;
    private static final double DEFAULT_CHANCE = 0.25;
    private static final double DEFAULT_VIEW = 24;
    private static final double DEFAULT_NEAR = 8;

    public AmbientParticlesFlag(String name) {
        super(name);
    }

    @Override
    protected AmbientParticles parse(String[] args) throws InvalidFlagFormat {
        if (args.length > 5) throw new InvalidFlagFormat("Usage: <particle> [count] [chance] [view] [near]");

        NamespacedKey key = NamespacedKey.fromString(args[0].toLowerCase());
        Particle particle = key == null ? null : Registry.PARTICLE_TYPE.get(key);
        if (particle == null) throw new InvalidFlagFormat("Unknown particle '" + args[0] + "'.");
        if (particle.getDataType() != Void.class) throw new InvalidFlagFormat("Particle '" + args[0] + "' needs extra data and is not supported.");

        int count = args.length > 1 ? parseInt(args[1], "count") : DEFAULT_COUNT;
        double chance = args.length > 2 ? parseDouble(args[2], "chance") : DEFAULT_CHANCE;
        double view = args.length > 3 ? parseDouble(args[3], "view distance") : DEFAULT_VIEW;
        double near = args.length > 4 ? parseDouble(args[4], "near radius") : DEFAULT_NEAR;
        if (count < 1) throw new InvalidFlagFormat("Count must be at least 1.");
        if (chance <= 0 || chance > 1) throw new InvalidFlagFormat("Chance must be greater than 0 and at most 1.");
        if (view <= 0) throw new InvalidFlagFormat("View distance must be positive.");
        if (near <= 0) throw new InvalidFlagFormat("Near radius must be positive.");

        return new AmbientParticles(particle, count, chance, view, near);
    }

    @Override
    protected String format(AmbientParticles value) {
        return value.particle().getKey().getKey() + " " + value.count() + " " + Utils.compactNumber(value.chance()) + " " + Utils.compactNumber(value.view()) + " " + Utils.compactNumber(value.near());
    }
}
