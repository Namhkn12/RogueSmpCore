package com.roguesmp.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

public final class Hub {

    private Hub() {
    }

    public static @NotNull Location spawn() {
        return MultiverseCoreApi.get().getWorldManager().getWorld("hub").get().getSpawnLocation();
    }
}
