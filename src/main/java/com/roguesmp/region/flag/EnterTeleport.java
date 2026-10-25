package com.roguesmp.region.flag;

import com.roguesmp.region.ServerRegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public record EnterTeleport(Target target, boolean fx) {

    public sealed interface Target {

        @Nullable Location resolve(Location from);

        String format();
    }

    private static @Nullable World worldOf(@Nullable NamespacedKey world, Location from) {
        return world == null ? from.getWorld() : Bukkit.getWorld(world);
    }

    public record RegionTarget(String regionId, @Nullable NamespacedKey world) implements Target {

        @Override
        public @Nullable Location resolve(Location from) {
            World target = worldOf(world, from);
            return target == null ? null : ServerRegionManager.getInstance().spawnLocation(target, regionId);
        }

        @Override
        public String format() {
            return "region " + regionId + (world == null ? "" : " " + world.asString());
        }
    }

    public record LocationTarget(@Nullable NamespacedKey world, double x, double y, double z, @Nullable Float yaw, @Nullable Float pitch) implements Target {

        @Override
        public @Nullable Location resolve(Location from) {
            World target = worldOf(world, from);
            if (target == null) return null;
            return new Location(target, x, y, z, yaw == null ? from.getYaw() : yaw, pitch == null ? from.getPitch() : pitch);
        }

        @Override
        public String format() {
            String position = TextValueFlag.number(x) + " " + TextValueFlag.number(y) + " " + TextValueFlag.number(z);
            if (world != null) position = world.asString() + " " + position;
            return yaw == null || pitch == null ? position : position + " " + TextValueFlag.number(yaw) + " " + TextValueFlag.number(pitch);
        }
    }
}
