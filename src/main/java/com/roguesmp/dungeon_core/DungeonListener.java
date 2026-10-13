package com.roguesmp.dungeon_core;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon_core.feature.QuestsFeature;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.WorldPos;
import com.roguesmp.world.Hub;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.event.RegionLeaveEvent;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Turns Bukkit gameplay inside a run's world into {@link DungeonSignal}s for that run.
 */
public class DungeonListener implements Listener {

    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30;

    private final DungeonManager manager;

    public DungeonListener(DungeonManager manager) {
        this.manager = manager;
    }

    /**
     * Puts a player who logged out inside a run back where they were, loading the world if needed, or sends them to the
     * hub when that run is gone or they are no longer a member.
     */
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
        Player player = event.getPlayer();
        DungeonInstance from = manager.getByWorld(event.getFrom());
        if (from != null) {
            manager.signal(from, new DungeonSignal.PlayerLeft(player));
            manager.unloadIfIdleLater(event.getFrom());
        }
        signal(player.getWorld(), new DungeonSignal.PlayerEntered(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        signal(player.getWorld(), new DungeonSignal.PlayerEntered(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!signal(world, new DungeonSignal.PlayerLeft(player))) return;
        manager.unloadIfIdleLater(world);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionEnter(RegionEnterEvent event) {
        signal(event.getWorld(), new DungeonSignal.RegionEntered(event.getPlayer(), event.getRegionId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionLeave(RegionLeaveEvent event) {
        signal(event.getWorld(), new DungeonSignal.RegionLeft(event.getPlayer(), event.getRegionId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent || !(event.getDamageSource().getCausingEntity() instanceof Player killer)) return;
        LivingEntity mob = event.getEntity();
        BaseEntity base = EntityUtils.getBaseEntity(mob);
        String mobId = base != null ? base.getId() : mob.getType().getKey().getKey();
        signal(mob.getWorld(), new DungeonSignal.MobKilled(killer, mob, mobId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        signal(player.getWorld(), new DungeonSignal.PlayerDied(player));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        World world = event.getPlayer().getWorld();
        if (manager.getByWorld(world) != null) event.setRespawnLocation(world.getSpawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuestCompleted(QuestCompletedEvent event) {
        if (!(event.getHolder() instanceof GroupHolder group) || !group.owner().equals(QuestsFeature.OWNER)) return;
        DungeonInstance instance = manager.get(group.id());
        if (instance != null) manager.signal(instance, new DungeonSignal.QuestCompleted(event.getQuest()));
    }

    /**
     * @return whether the world belongs to a run
     */
    private boolean signal(World world, DungeonSignal signal) {
        DungeonInstance instance = manager.getByWorld(world);
        if (instance == null) return false;
        manager.signal(instance, signal);
        return true;
    }
}
