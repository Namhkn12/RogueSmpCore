package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BlockPos(UUID world, int x, int y, int z) {

    public static final Codec<BlockPos> CODEC = Codec.STRING.comapFlatMap(BlockPos::parse, BlockPos::serialize);

    public static BlockPos of(Location location) {
        return new BlockPos(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static DataResult<BlockPos> parse(String serialized) {
        String[] parts = serialized.split(";");
        if (parts.length != 4) return DataResult.error("Malformed block position: " + serialized);
        try {
            return DataResult.success(new BlockPos(
                    UUID.fromString(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            ));
        } catch (IllegalArgumentException e) {
            return DataResult.error("Malformed block position: " + serialized);
        }
    }

    public String serialize() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public @Nullable World getWorld() {
        return Bukkit.getWorld(world);
    }

    public @Nullable Location toLocation() {
        World bukkitWorld = getWorld();
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z);
    }

    public @Nullable Block getBlock() {
        World bukkitWorld = getWorld();
        return bukkitWorld == null ? null : bukkitWorld.getBlockAt(x, y, z);
    }

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(world, x + dx, y + dy, z + dz);
    }

    public BlockPos relative(BlockFace face) {
        return new BlockPos(world, x + face.getModX(), y + face.getModY(), z + face.getModZ());
    }

    public boolean matches(Location location) {
        return location.getWorld() != null
                && location.getWorld().getUID().equals(world)
                && location.getBlockX() == x
                && location.getBlockY() == y
                && location.getBlockZ() == z;
    }
}
