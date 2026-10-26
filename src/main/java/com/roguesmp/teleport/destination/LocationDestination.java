package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinations;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record LocationDestination(@Nullable Key world, double x, double y, double z, @Nullable Float yaw, @Nullable Float pitch) implements TeleportDestination {

    public static final String TYPE_KEY = "location";

    public static final Codec<LocationDestination> CODEC = Codec.composite(
            Codec.KEY.optionalFieldOf("world").forGetter(destination -> Optional.ofNullable(destination.world())),
            Codec.DOUBLE.fieldOf("x").forGetter(LocationDestination::x),
            Codec.DOUBLE.fieldOf("y").forGetter(LocationDestination::y),
            Codec.DOUBLE.fieldOf("z").forGetter(LocationDestination::z),
            Codec.FLOAT.optionalFieldOf("yaw").forGetter(destination -> Optional.ofNullable(destination.yaw())),
            Codec.FLOAT.optionalFieldOf("pitch").forGetter(destination -> Optional.ofNullable(destination.pitch())),
            (world, x, y, z, yaw, pitch) -> new LocationDestination(world.orElse(null), x, y, z, yaw.orElse(null), pitch.orElse(null))
    );

    public static LocationDestination of(Location location) {
        return new LocationDestination(location.getWorld().getKey(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable Location resolve(Player player) {
        World target = TeleportDestinations.worldOf(world, player);
        if (target == null) return null;
        Location from = player.getLocation();
        return new Location(target, x, y, z, yaw == null ? from.getYaw() : yaw, pitch == null ? from.getPitch() : pitch);
    }

    @Override
    public String format() {
        String position = Utils.compactNumber(x) + " " + Utils.compactNumber(y) + " " + Utils.compactNumber(z);
        if (world != null) position = world.asString() + " " + position;
        return yaw == null || pitch == null ? position : position + " " + Utils.compactNumber(yaw) + " " + Utils.compactNumber(pitch);
    }
}
