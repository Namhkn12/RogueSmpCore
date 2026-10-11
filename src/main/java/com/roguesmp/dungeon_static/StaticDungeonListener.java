package com.roguesmp.dungeon_static;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.DailyResetEvent;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.WorldPos;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StaticDungeonListener implements Listener {

    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30;

    private final StaticDungeonManager manager;

    public StaticDungeonListener(StaticDungeonManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        manager.expireDue();
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

        StaticDungeonInstance instance = manager.get(instanceId);
        if (instance != null && instance.isMember(playerId) && !instance.isExpired(System.currentTimeMillis())) {
            try {
                World world = manager.worlds().load(instance.template(), instanceId).get(WORLD_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setSpawnLocation(lastLocation.toLocation(world));
                return;
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Failed to load static dungeon {} for login of {}", instanceId, playerId, e);
            }
        }

        MultiverseCoreApi.get().getWorldManager().getDefaultWorld().peek(hub -> event.setSpawnLocation(hub.getSpawnLocation()));
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        manager.unloadIfIdleLater(event.getFrom());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.unloadIfIdleLater(event.getPlayer().getWorld());
    }
}
