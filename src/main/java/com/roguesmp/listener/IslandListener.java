package com.roguesmp.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.roguesmp.island.IslandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

/**
 * For events related to island worlds
 */
public class IslandListener implements Listener {

    private final IslandManager islandManager;

    public IslandListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!islandManager.isInIslandWorld(player)) return;
        //Evict homeless player upon joining
        Location spawnLocation = islandManager.getIslandSpawnLocation(player);
        if (spawnLocation == null) {
            MultiverseCoreApi api = islandManager.getIslandWorldManager().getMultiverseApi();
            api.getWorldManager().getDefaultWorld().peek(loadedMultiverseWorld -> {
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                api.getSafetyTeleporter().to(loadedMultiverseWorld.getSpawnLocation()).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(player);
                player.sendMessage(Component.text("Bạn không có điểm hồi sinh nào nên sẽ được dịch chuyển về hub.", NamedTextColor.YELLOW));
            });
        } else { //Player will always spawn on their island upon logging in
            MultiverseCoreApi api = islandManager.getIslandWorldManager().getMultiverseApi();
            api.getWorldManager().getDefaultWorld().peek(loadedMultiverseWorld -> {
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                api.getSafetyTeleporter().to(loadedMultiverseWorld.getSpawnLocation()).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(player);
            });
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        if (!islandManager.isInIslandWorld(player)) return;
        Location spawnLocation = islandManager.getIslandSpawnLocation(player);
        if (spawnLocation == null) {
            MultiverseCoreApi api = islandManager.getIslandWorldManager().getMultiverseApi();
            api.getWorldManager().getDefaultWorld().peek(loadedMultiverseWorld -> {
                api.getSafetyTeleporter().to(loadedMultiverseWorld.getSpawnLocation()).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(player);
                player.sendMessage(Component.text("Bạn không có điểm hồi sinh nào nên sẽ được dịch chuyển về hub.", NamedTextColor.YELLOW));
            });
            return;
        }
        player.teleport(spawnLocation);
    }

    @EventHandler
    public void onPlayerFellIntoVoid(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.hasChangedBlock() && player.getY() <= -65) {
            if (!islandManager.isInIslandWorld(player)) return;
            Location spawnLocation = islandManager.getIslandSpawnLocation(player);
            if (spawnLocation == null) {
                MultiverseCoreApi api = islandManager.getIslandWorldManager().getMultiverseApi();
                api.getWorldManager().getDefaultWorld().peek(loadedMultiverseWorld -> {
                    api.getSafetyTeleporter().to(loadedMultiverseWorld.getSpawnLocation()).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(player);
                    player.sendMessage(Component.text("Bạn không có điểm hồi sinh nào nên sẽ được dịch chuyển về hub.", NamedTextColor.YELLOW));
                });
                player.setFallDistance(0);
                player.setVelocity(new Vector(0, 0, 0));
                return;
            }
            player.setFallDistance(0);
            player.setVelocity(new Vector(0, 0, 0));
            player.teleport(spawnLocation);
        }
    }
}
