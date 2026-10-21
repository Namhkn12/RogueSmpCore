package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockType;
import com.roguesmp.block.event.SmpBlockInteractEvent;
import com.roguesmp.block.event.SmpBlockPlaceEvent;
import com.roguesmp.block.event.SmpBlockRemoveEvent;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import com.roguesmp.island.IslandManager;
import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WorldPos;
import com.roguesmp.world.Hub;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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

        Location hub = Hub.spawn();
        event.setSpawnLocation(hub);
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
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        clearLegacyIslandRespawn(player);

        World world = player.getWorld();
        if (islandManager.getIslandWorldManager().isIslandWorld(world)) {
            Location hub = isGuest(player, world) ? hubSpawn() : null;
            event.setRespawnLocation(hub != null ? hub : islandSpawn(world));
            return;
        }

        World respawnWorld = event.getRespawnLocation().getWorld();
        if (respawnWorld == null || !islandManager.getIslandWorldManager().isIslandWorld(respawnWorld)) return;

        Location hub = hubSpawn();
        event.setRespawnLocation(hub);
    }

    private void clearLegacyIslandRespawn(Player player) {
        Location stored = player.getRespawnLocation();
        if (stored != null && stored.getWorld() != null && islandManager.getIslandWorldManager().isIslandWorld(stored.getWorld())) {
            player.setRespawnLocation(null);
        }
    }

    private boolean isGuest(Player player, World world) {
        IslandData islandData = islandManager.getIslandByWorld(world);
        return islandData == null || !islandData.isMember(player.getUniqueId());
    }

    private Location islandSpawn(World world) {
        return world.getSpawnLocation();
    }

    private Location hubSpawn() {
        return Hub.spawn();
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
        if (isGuest(player, world)) {
            islandManager.getTeleportService().sendToHub(player);
            return;
        }
        player.teleport(islandSpawn(world));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockLimitCheck(SmpBlockPlaceEvent event) {
        IslandData island = islandManager.getIslandByWorld(event.getTarget().getWorld());
        if (island == null) return;

        BlockType<?, ?> blockType = event.getBlock().getType();
        Integer limit = IslandLevel.of(island.getLevel()).blockLimits().get(blockType.key());
        if (limit == null || island.getBlockCount(blockType.key()) < limit) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(Utils.fromString("<red>Đảo đã đạt giới hạn <yellow>" + limit + " " + blockType.displayName() + "<red>. Nâng cấp đảo để đặt thêm!"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaced(SmpBlockPlaceEvent event) {
        IslandData island = islandManager.getIslandByWorld(event.getTarget().getWorld());
        if (island != null) island.changeBlockCount(event.getBlock().getType().key(), 1);
    }

    @EventHandler
    public void onBlockRemoved(SmpBlockRemoveEvent event) {
        World world = event.getBlock().getLocation().getWorld();
        IslandData island = world == null ? null : islandManager.getIslandByWorld(world);
        if (island != null) island.changeBlockCount(event.getBlock().getType().key(), -1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(SmpBlockInteractEvent event) {
        Player player = event.getPlayer();
        IslandData island = islandManager.getIslandByWorld(player.getWorld());
        if (island == null || island.isMember(player.getUniqueId()) || player.hasPermission(Permissions.ISLAND_ADMIN.node())) return;

        event.setCancelled(true);
        player.sendActionBar(Utils.fromString("<red>Chỉ thành viên đảo mới có thể sử dụng khối này"));
    }
}
