package com.roguesmp.region.flag;

import com.roguesmp.region.ServerRegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public record EnterTeleport(Target target, boolean fx) {

    public sealed interface Target {

        @Nullable Location resolve(Location from);

        String format();
    }

    private static @Nullable World worldOf(@Nullable String worldName, Location from) {
        return worldName == null ? from.getWorld() : Bukkit.getWorld(worldName);
    }

    public record RegionTarget(@Nullable String worldName, String regionId) implements Target {

        @Override
        public @Nullable Location resolve(Location from) {
            World world = worldOf(worldName, from);
            return world == null ? null : ServerRegionManager.getInstance().spawnLocation(world, regionId);
        }

        @Override
        public String format() {
            return "region:" + (worldName == null ? "" : worldName + ":") + regionId;
        }
    }

    public record LocationTarget(@Nullable String worldName, double x, double y, double z, @Nullable Float yaw, @Nullable Float pitch) implements Target {

        @Override
        public @Nullable Location resolve(Location from) {
            World world = worldOf(worldName, from);
            if (world == null) return null;
            return new Location(world, x, y, z, yaw == null ? from.getYaw() : yaw, pitch == null ? from.getPitch() : pitch);
        }

        @Override
        public String format() {
            String position = TextValueFlag.number(x) + " " + TextValueFlag.number(y) + " " + TextValueFlag.number(z);
            if (worldName != null) position = worldName + " " + position;
            return yaw == null || pitch == null ? position : position + " " + TextValueFlag.number(yaw) + " " + TextValueFlag.number(pitch);
        }
    }
}
