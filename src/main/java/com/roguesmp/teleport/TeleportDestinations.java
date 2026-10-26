package com.roguesmp.teleport;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import com.roguesmp.teleport.destination.*;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TeleportDestinations {

    public static final Codec<LocationDestination> LOCATION = register(LocationDestination.TYPE_KEY, LocationDestination.CODEC);
    public static final Codec<RegionDestination> REGION = register(RegionDestination.TYPE_KEY, RegionDestination.CODEC);
    public static final Codec<WorldSpawnDestination> WORLD_SPAWN = register(WorldSpawnDestination.TYPE_KEY, WorldSpawnDestination.CODEC);
    public static final Codec<HubDestination> HUB = register(HubDestination.TYPE_KEY, HubDestination.CODEC);
    public static final Codec<WarpDestination> WARP = register(WarpDestination.TYPE_KEY, WarpDestination.CODEC);
    public static final Codec<PlayerDestination> PLAYER = register(PlayerDestination.TYPE_KEY, PlayerDestination.CODEC);

    public static void loadClass() {

    }

    public static @Nullable World worldOf(@Nullable Key world, Player player) {
        return world == null ? player.getWorld() : Bukkit.getWorld(world);
    }

    private static <T extends TeleportDestination> Codec<T> register(String key, Codec<T> codec) {
        return Registries.TELEPORT_DESTINATION_CODEC.register(key, codec);
    }
}
