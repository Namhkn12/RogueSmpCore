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
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldedit.math.BlockVector3;
import com.roguesmp.world.Hub;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.block.Block;
import com.roguesmp.constant.Keys;
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
    public void onMarkerInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(Keys.REVIVE_POINT)) event.setCancelled(true);
    }

    @EventHandler
    public void onMarkerDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(Keys.REVIVE_POINT)) event.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        World world = event.getPlayer().getWorld();
        DungeonInstance instance = manager.getByWorld(world);
        if (instance == null) return;

        Location bed = bedSpawn(event.getPlayer(), instance, world);
        event.setRespawnLocation(bed != null ? bed : world.getSpawnLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = manager.getByWorld(player.getWorld());
        if (instance == null || event.getCause() != PlayerSetSpawnEvent.Cause.BED || event.getLocation() == null) return;

        event.setCancelled(true);
        Location bed = event.getLocation();
        if (!allowsBedSpawn(bed)) {
            player.sendActionBar(Utils.fromString("<red>Không thể đặt điểm hồi sinh ở đây"));
            return;
        }
        instance.setBedSpawn(player.getUniqueId(), BlockVector3.at(bed.getBlockX(), bed.getBlockY(), bed.getBlockZ()));
        manager.save(instance);
        player.sendActionBar(Utils.fromString("<green>Đã đặt điểm hồi sinh trong hầm ngục"));
    }

    private @Nullable Location bedSpawn(Player player, DungeonInstance instance, World world) {
        BlockVector3 position = instance.bedSpawn(player.getUniqueId());
        if (position == null) return null;

        Block bed = world.getBlockAt(position.x(), position.y(), position.z());
        if (!allowsBedSpawn(bed.getLocation())) return null;
        if (!Tag.BEDS.isTagged(bed.getType())) {
            instance.setBedSpawn(player.getUniqueId(), null);
            manager.save(instance);
            player.sendMessage(Utils.fromString("<red>Giường của bạn trong hầm ngục đã bị phá"));
            return null;
        }
        Location spot = standingSpot(bed);
        if (spot == null) player.sendMessage(Utils.fromString("<red>Giường của bạn trong hầm ngục đang bị chặn"));
        return spot;
    }

    private static @Nullable Location standingSpot(Block bed) {
        if (fitsPlayer(bed.getRelative(BlockFace.UP))) return bed.getLocation().add(0.5, 0.5625, 0.5);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -1; dy--) {
                    Block feet = bed.getRelative(dx, dy, dz);
                    if (fitsPlayer(feet) && feet.getRelative(BlockFace.DOWN).isSolid()) return feet.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }

    private static boolean fitsPlayer(Block feet) {
        return feet.isPassable() && !feet.isLiquid() && feet.getRelative(BlockFace.UP).isPassable();
    }

    private static boolean allowsBedSpawn(Location location) {
        return ServerRegionManager.getInstance().regionsAt(location).testState(null, SmpRegionFlags.BED_SPAWN);
    }
}
