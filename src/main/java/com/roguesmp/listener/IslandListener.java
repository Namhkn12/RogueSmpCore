package com.roguesmp.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.WorldPos;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class IslandListener implements Listener {

    private static final int VOID_Y = -65;
    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30;

    private final IslandManager islandManager;

    public IslandListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        UUID uuid = event.getConnection().getProfile().getId();
        PlayerData playerData = islandManager.getPlayerManager().getDataManager().getData(uuid);
        if (playerData == null) return;

        UUID islandId = playerData.getIslandId();
        IslandData loadedData = islandManager.getIslandDataManager().loadIslandData(islandId);
        IslandData islandData = loadedData == null ? null : islandManager.getIslandDataManager().cache(loadedData);

        WorldPos lastLocation = playerData.getLastLocation();
        if (lastLocation == null) return;

        UUID lastIslandId = islandManager.getIslandWorldManager().getIslandId(lastLocation.world().asString());
        if (lastIslandId == null) return;

        if (islandData != null && !islandData.isArchived() && lastIslandId.equals(islandId)) {
            try {
                World world = islandManager.getIslandWorldManager().loadIslandWorld(islandId).get(WORLD_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setSpawnLocation(lastLocation.toLocation(world));
                return;
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Failed to load island world {} for login of {}", islandId, uuid, e);
            }
        }

        islandManager.getTeleportService().getMultiverseApi().getWorldManager().getDefaultWorld()
                .peek(hub -> event.setSpawnLocation(hub.getSpawnLocation()));
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        releaseIfVisitorsGone(event.getFrom(), event.getPlayer(), false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        releaseIfVisitorsGone(event.getPlayer().getWorld(), event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        if (!islandManager.getIslandWorldManager().isIslandWorld(player.getWorld())) return;

        returnToIslandSpawn(player);
    }

    @EventHandler
    public void onPlayerFellIntoVoid(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedBlock() || player.getY() > VOID_Y) return;
        if (!islandManager.getIslandWorldManager().isIslandWorld(player.getWorld())) return;

        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0, 0));
        returnToIslandSpawn(player);
    }

    private void releaseIfVisitorsGone(World world, Player leaving, boolean leavingServer) {
        IslandData islandData = islandManager.getIslandByWorld(world);
        if (islandData == null) return;

        for (Player other : world.getPlayers()) {
            if (!other.getUniqueId().equals(leaving.getUniqueId())) return;
        }
        islandManager.releaseIslandIfUnoccupied(islandData, leavingServer ? leaving.getUniqueId() : null);
    }

    private void returnToIslandSpawn(Player player) {
        World world = player.getWorld();
        IslandData islandData = islandManager.getIslandByWorld(world);
        Location spawnLocation = islandData == null ? null : islandData.getSpawnLocationWorld(world);
        if (spawnLocation == null) {
            player.sendMessage(Component.text("Bạn không có điểm hồi sinh nào nên sẽ được dịch chuyển về hub.", NamedTextColor.YELLOW));
            islandManager.getTeleportService().sendToHub(player);
            return;
        }
        player.teleport(spawnLocation);
    }
}
