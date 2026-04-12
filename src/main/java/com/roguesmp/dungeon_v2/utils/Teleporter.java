package com.roguesmp.dungeon_v2.utils;

import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class Teleporter {

    private Teleporter() {}

    public static void teleport(Player player, SerializableLocation location) {
        World world = Bukkit.getWorld(location.getWorld());
        if (world == null) return;

        Location loc = new Location(world,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );

        player.teleport(loc);
    }

    public static void teleport(Player player, Location location){
        player.teleport(location);
    }

    public static void teleportAll(List<Player> players, Location location) {
        players.forEach(p -> p.teleport(location));
    }

    public static void teleportAllByID(List<UUID> uuids, Location location) {
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(location);
            }
        }
    }
}
