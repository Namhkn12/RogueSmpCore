package com.roguesmp.worldboss.encounter;

import com.roguesmp.registry.Registries;

import java.util.function.Supplier;

public class WorldBossEncounters {

    public static void loadClass() {

    }

    private static <T extends WorldBossEncounter> Supplier<T> register(String id, Supplier<T> factory) {
        return Registries.WORLD_BOSS_ENCOUNTER.register(id, factory);
    }
}
