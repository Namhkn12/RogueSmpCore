package com.roguesmp.dungeon.listener;

import org.bukkit.event.world.WorldUnloadEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.WorldPos;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.roguesmp.world.Hub;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import com.roguesmp.block.event.SmpBlockPlaceEvent;
import com.roguesmp.utils.Utils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DungeonListener implements Listener {

    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30;

    private final DungeonManager manager;

    public DungeonListener(DungeonManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        UUID playerId = event.getConnection().getProfile().getId();
        PlayerData playerData = PlayerManager.getInstance().getDataManager().getData(playerId);
        if (playerData == null) return;

        WorldPos lastLocation = playerData.getLastLocation();
        if (lastLocation == null) return;

        UUID instanceId = manager.worlds().getInstanceId(lastLocation.world().asString());
        if (instanceId == null) return;

        DungeonInstance instance = manager.get(instanceId);
        if (instance != null && instance.isMember(playerId)) {
            try {
                World world = manager.worlds().load(instance.template(), instanceId).get(WORLD_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setSpawnLocation(lastLocation.toLocation(world));
                return;
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Failed to load dungeon run {} for login of {}", instanceId, playerId, e);
            }
        }
        event.setSpawnLocation(Hub.spawn());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (manager.getByWorld(event.getFrom()) != null) manager.unloadIfIdleLater(event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        if (manager.getByWorld(world) != null) manager.unloadIfIdleLater(world);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAdd(EntityAddToWorldEvent event) {
        manager.trackedMobs().add(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        manager.trackedMobs().remove(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        manager.trackedMobs().clear(event.getWorld());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmpBlockPlace(SmpBlockPlaceEvent event) {
        if (!manager.worlds().isDungeonWorld(event.getTarget().getWorld())) return;

        event.setCancelled(true);
        event.getPlayer().sendActionBar(Utils.fromString("<red>Không thể đặt khối này trong hầm ngục"));
    }

    @EventHandler
    public void onPortalTravel(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        boolean portal = cause == PlayerTeleportEvent.TeleportCause.END_PORTAL || cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY;
        if (portal && manager.getByWorld(event.getPlayer().getWorld()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (manager.getByWorld(event.getWorld()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        World world = event.getPlayer().getWorld();
        if (manager.getByWorld(world) != null) event.setRespawnLocation(world.getSpawnLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getCause() == PlayerSetSpawnEvent.Cause.BED && manager.getByWorld(event.getPlayer().getWorld()) != null) event.setCancelled(true);
    }
}
