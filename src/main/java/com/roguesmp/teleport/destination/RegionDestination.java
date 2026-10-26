package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinations;
import org.bukkit.Location;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record RegionDestination(String region, @Nullable Key world) implements TeleportDestination {

    public static final String TYPE_KEY = "region";

    public static final Codec<RegionDestination> CODEC = Codec.composite(
            Codec.STRING.fieldOf("region").forGetter(RegionDestination::region),
            Codec.KEY.optionalFieldOf("world").forGetter(destination -> Optional.ofNullable(destination.world())),
            (region, world) -> new RegionDestination(region, world.orElse(null))
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable Location resolve(Player player) {
        World target = TeleportDestinations.worldOf(world, player);
        return target == null ? null : ServerRegionManager.getInstance().spawnLocation(target, region);
    }

    @Override
    public boolean isAvailable(Player player) {
        World target = TeleportDestinations.worldOf(world, player);
        return target != null && ServerRegionManager.getInstance().get(target, region) != null;
    }

    @Override
    public String format() {
        return "region " + region + (world == null ? "" : " " + world.asString());
    }
}
