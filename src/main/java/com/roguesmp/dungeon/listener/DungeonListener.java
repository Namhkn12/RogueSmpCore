package com.roguesmp.dungeon.listener;

import org.bukkit.event.world.WorldUnloadEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.TrackedMobs;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.feature.QuestsFeature;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.WorldPos;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldedit.math.BlockVector3;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.block.BlockRef;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.world.Hub;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.event.RegionLeaveEvent;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.bukkit.event.Event;
import org.bukkit.Tag;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import com.roguesmp.constant.Keys;
import com.roguesmp.block.event.SmpBlockPlaceEvent;
import com.roguesmp.utils.Utils;

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent) return;
        LivingEntity mob = event.getEntity();
        DungeonInstance instance = manager.getByWorld(mob.getWorld());
        if (instance == null) return;

        BaseEntity base = EntityUtils.getBaseEntity(mob);
        String mobId = base != null ? base.getId() : mob.getType().getKey().getKey();
        Player killer = event.getDamageSource().getCausingEntity() instanceof Player player ? player : null;
        String tag = TrackedMobs.tagOf(mob);

        manager.fire(instance, new DungeonEvent.MobDied(killer, mob, mobId, tag));
        if (killer != null) manager.fire(instance, new DungeonEvent.MobKilled(killer, mob, mobId));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;
        if (manager.getByWorld(mob.getWorld()) == null) return;
        BaseEntity base = EntityUtils.getBaseEntity(mob);
        String mobId = base != null ? base.getId() : mob.getType().getKey().getKey();
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : mob.getHealth();
        double after = Math.max(0, mob.getHealth() - event.getFinalDamage());
        Player attacker = event.getDamageSource().getCausingEntity() instanceof Player player ? player : null;
        fire(mob.getWorld(), new DungeonEvent.MobDamaged(attacker, mob, mobId, event.getFinalDamage(), max <= 0 ? 0 : after / max * 100));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() == Action.PHYSICAL) {
            if (isPressurePlate(block) && !isPowered(block.getBlockData())) {
                fire(block.getWorld(), new DungeonEvent.RedstoneActivated(event.getPlayer(), block, true));
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        fire(block.getWorld(), new DungeonEvent.BlockInteracted(event.getPlayer(), block, event));
        if (event.useInteractedBlock() == Event.Result.DENY || !(block.getBlockData() instanceof Switch button)) return;
        boolean lever = block.getType() == Material.LEVER;
        if (!lever && button.isPowered()) return;
        fire(block.getWorld(), new DungeonEvent.RedstoneActivated(event.getPlayer(), block, !lever || !button.isPowered()));
    }

    private static boolean isPowered(org.bukkit.block.data.BlockData data) {
        if (data instanceof AnaloguePowerable analogue) return analogue.getPower() > 0;
        return data instanceof Powerable powerable && powerable.isPowered();
    }

    private static boolean isPressurePlate(Block block) {
        return Tag.PRESSURE_PLATES.isTagged(block.getType()) || block.getType() == Material.TRIPWIRE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmpBlockPlace(SmpBlockPlaceEvent event) {
        if (!manager.worlds().isDungeonWorld(event.getTarget().getWorld())) return;

        event.setCancelled(true);
        event.getPlayer().sendActionBar(Utils.fromString("<red>Không thể đặt khối này trong hầm ngục"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        fire(block.getWorld(), new DungeonEvent.BlockBroken(event.getPlayer(), block, event));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (isCustomPlacement(event)) return;
        fire(block.getWorld(), new DungeonEvent.BlockPlaced(event.getPlayer(), block, BlockRef.VANILLA_PREFIX + block.getType().getKey().getKey(), event));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomBlockPlace(SmpBlockPlaceEvent event) {
        Block block = event.getTarget();
        fire(block.getWorld(), new DungeonEvent.BlockPlaced(event.getPlayer(), block, event.getBlock().getId(), event));
    }

    private static boolean isCustomPlacement(BlockPlaceEvent event) {
        return event.getBlockPlaced().getType() == Material.BARRIER && ItemStackUtils.getBaseId(event.getItemInHand()) != null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        String itemId = ItemRef.idOf(item);
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
