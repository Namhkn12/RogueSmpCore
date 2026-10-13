package com.roguesmp.dungeon.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.feature.QuestsFeature;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import com.roguesmp.constant.Keys;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DungeonListener implements Listener {

    private static final long WORLD_LOAD_TIMEOUT_SECONDS = 30;

    private static final Set<Material> PORTALS = Set.of(Material.END_GATEWAY, Material.NETHER_PORTAL, Material.END_PORTAL);

    private final DungeonManager manager;
    private final Map<UUID, Material> insidePortal = new HashMap<>();

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
        Player player = event.getPlayer();
        DungeonInstance from = manager.getByWorld(event.getFrom());
        if (from != null) {
            manager.fire(from, new DungeonEvent.PlayerLeft(player));
            manager.unloadIfIdleLater(event.getFrom());
        }
        fire(player.getWorld(), new DungeonEvent.PlayerEntered(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        fire(player.getWorld(), new DungeonEvent.PlayerEntered(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        insidePortal.remove(player.getUniqueId());
        World world = player.getWorld();
        if (!fire(world, new DungeonEvent.PlayerLeft(player))) return;
        manager.unloadIfIdleLater(world);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionEnter(RegionEnterEvent event) {
        fire(event.getWorld(), new DungeonEvent.RegionEntered(event.getPlayer(), event.getRegionId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionLeave(RegionLeaveEvent event) {
        fire(event.getWorld(), new DungeonEvent.RegionLeft(event.getPlayer(), event.getRegionId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent || !(event.getDamageSource().getCausingEntity() instanceof Player killer)) return;
        LivingEntity mob = event.getEntity();
        BaseEntity base = EntityUtils.getBaseEntity(mob);
        String mobId = base != null ? base.getId() : mob.getType().getKey().getKey();
        fire(mob.getWorld(), new DungeonEvent.MobKilled(killer, mob, mobId));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) return;
        fire(block.getWorld(), new DungeonEvent.BlockInteracted(event.getPlayer(), block, event));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        fire(block.getWorld(), new DungeonEvent.BlockBroken(event.getPlayer(), block, event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        String itemId = item.getPersistentDataContainer().getOrDefault(Keys.ITEM_ID, PersistentDataType.STRING, item.getType().getKey().getKey());
        fire(player.getWorld(), new DungeonEvent.ItemPickedUp(player, item, itemId, item.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        if (manager.getByWorld(player.getWorld()) == null) {
            insidePortal.remove(player.getUniqueId());
            return;
        }

        Block portal = touchedPortal(player, event.getTo());
        if (portal == null) {
            insidePortal.remove(player.getUniqueId());
            return;
        }
        if (insidePortal.put(player.getUniqueId(), portal.getType()) == portal.getType()) return;
        fire(player.getWorld(), new DungeonEvent.PortalEntered(player, portal.getType(), portal.getLocation()));
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        fire(player.getWorld(), new DungeonEvent.PlayerDied(player, event));
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
        if (manager.getByWorld(world) != null) event.setRespawnLocation(world.getSpawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuestCompleted(QuestCompletedEvent event) {
        if (!(event.getHolder() instanceof GroupHolder group) || !group.owner().equals(QuestsFeature.OWNER)) return;
        DungeonInstance instance = manager.get(group.id());
        if (instance != null) manager.fire(instance, new DungeonEvent.QuestCompleted(event.getQuest()));
    }

    private static @Nullable Block touchedPortal(Player player, Location to) {
        BoundingBox box = player.getBoundingBox().shift(to.toVector().subtract(player.getLocation().toVector()));
        World world = player.getWorld();
        for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
            for (int y = (int) Math.floor(box.getMinY()); y <= (int) Math.floor(box.getMaxY()); y++) {
                for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (PORTALS.contains(block.getType())) return block;
                }
            }
        }
        return null;
    }

    private boolean fire(World world, DungeonEvent event) {
        DungeonInstance instance = manager.getByWorld(world);
        if (instance == null) return false;
        manager.fire(instance, event);
        return true;
    }
}
