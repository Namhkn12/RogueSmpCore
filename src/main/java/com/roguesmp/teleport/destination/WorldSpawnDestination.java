package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinations;
import org.bukkit.Location;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record WorldSpawnDestination(@Nullable Key world) implements TeleportDestination {

    public static final String TYPE_KEY = "world_spawn";

    public static final Codec<WorldSpawnDestination> CODEC = Codec.KEY.optionalFieldOf("world")
            .xmap(world -> new WorldSpawnDestination(world.orElse(null)), destination -> Optional.ofNullable(destination.world()))
            .codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable Location resolve(Player player) {
        World target = TeleportDestinations.worldOf(world, player);
        return target == null ? null : target.getSpawnLocation();
    }

    @Override
    public String format() {
        return "spawn" + (world == null ? "" : " " + world.asString());
    }
}
