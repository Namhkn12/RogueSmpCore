package com.roguesmp.dungeon.listener;

import com.roguesmp.block.BlockRef;
import com.roguesmp.block.event.SmpBlockPlaceEvent;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.TrackedMobs;
import com.roguesmp.dungeon.feature.QuestsFeature;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.region.event.RegionEnterEvent;
import com.roguesmp.region.event.RegionLeaveEvent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonInstance;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BukkitEventAdapters {

    private static final Set<Material> PORTALS = Set.of(Material.END_GATEWAY, Material.NETHER_PORTAL, Material.END_PORTAL);
    private static final Map<UUID, Material> INSIDE_PORTAL = new HashMap<>();
    private static final String[] BLOCK_POSITION = {"x", "y", "z"};

    private static final Listener LISTENER = new Listener() {
    };

    private static final BukkitEventAdapter.Emitter EMITTER = new BukkitEventAdapter.Emitter() {
        @Override
        public void fire(@Nullable World world, DungeonEvent event) {
            fire(world == null ? null : DungeonManager.getInstance().getByWorld(world), event);
        }

        @Override
        public void fire(@Nullable DungeonInstance instance, DungeonEvent event) {
            if (instance != null) DungeonManager.getInstance().fire(instance, event);
        }
    };

    static {
        registerFeatureEvents();
        registerScriptEvents();
    }

    private BukkitEventAdapters() {
    }

    public static void loadClass() {
    }

    public static Collection<BukkitEventAdapter<?>> adapters() {
        return Registries.DUNGEON_BUKKIT_EVENT.getAll().values();
    }

    public static boolean isCancellable(String eventId) {
        return adapters().stream().flatMap(adapter -> adapter.emits().stream())
                .anyMatch(emits -> emits.id().equals(eventId) && emits.cancellable());
    }

    // Adapters only scripts use are skipped unless some loaded dungeon listens to one of their events.
    public static void listen(Set<String> usedEvents) {
        HandlerList.unregisterAll(LISTENER);
        for (BukkitEventAdapter<?> adapter : adapters()) {
            boolean used = adapter.always() || adapter.emits().stream().anyMatch(emits -> usedEvents.contains(emits.id()));
            if (!used) continue;
            Bukkit.getPluginManager().registerEvent(adapter.type(), LISTENER, adapter.priority(),
                    (listener, event) -> adapter.translate(event, EMITTER), RogueSmpCore.getInstance(), adapter.ignoreCancelled());
        }
    }

    private static void registerFeatureEvents() {
        register(BukkitEventAdapter.custom("join", PlayerJoinEvent.class).always()
                .emits("player_entered")
                .translate((event, emit) -> emit.fire(event.getPlayer().getWorld(), new DungeonEvent.PlayerEntered(event.getPlayer()))));
        register(BukkitEventAdapter.custom("quit", PlayerQuitEvent.class).always()
                .emits("player_left")
                .translate((event, emit) -> {
                    INSIDE_PORTAL.remove(event.getPlayer().getUniqueId());
                    emit.fire(event.getPlayer().getWorld(), new DungeonEvent.PlayerLeft(event.getPlayer()));
                }));
        register(BukkitEventAdapter.custom("changed_world", PlayerChangedWorldEvent.class).always()
                .emits("player_left").emits("player_entered")
                .translate((event, emit) -> {
                    emit.fire(event.getFrom(), new DungeonEvent.PlayerLeft(event.getPlayer()));
                    emit.fire(event.getPlayer().getWorld(), new DungeonEvent.PlayerEntered(event.getPlayer()));
                }));
        register(BukkitEventAdapter.custom("region_entered", RegionEnterEvent.class).always()
                .emits("region_entered")
                .translate((event, emit) -> emit.fire(event.getWorld(), new DungeonEvent.RegionEntered(event.getPlayer(), event.getRegionId()))));
        register(BukkitEventAdapter.custom("region_left", RegionLeaveEvent.class).always()
                .emits("region_left")
                .translate((event, emit) -> emit.fire(event.getWorld(), new DungeonEvent.RegionLeft(event.getPlayer(), event.getRegionId()))));
        register(BukkitEventAdapter.custom("mob_death", EntityDeathEvent.class).always()
                .emits("mob_died", "tag").emits("mob_killed")
                .translate((event, emit) -> {
                    if (event instanceof PlayerDeathEvent) return;
                    LivingEntity mob = event.getEntity();
                    String mobId = mobId(mob);
                    Player killer = event.getDamageSource().getCausingEntity() instanceof Player player ? player : null;
                    emit.fire(mob.getWorld(), new DungeonEvent.MobDied(killer, mob, mobId, TrackedMobs.tagOf(mob)));
                    if (killer != null) emit.fire(mob.getWorld(), new DungeonEvent.MobKilled(killer, mob, mobId));
                }));
        register(BukkitEventAdapter.custom("mob_damage", EntityDamageEvent.class).always().ignoreCancelled()
                .emits("mob_damaged", "damage", "health_percent")
                .translate((event, emit) -> {
                    if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;
                    if (DungeonManager.getInstance().getByWorld(mob.getWorld()) == null) return;
                    AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
                    double max = maxHealth != null ? maxHealth.getValue() : mob.getHealth();
                    double after = Math.max(0, mob.getHealth() - event.getFinalDamage());
                    Player attacker = event.getDamageSource().getCausingEntity() instanceof Player player ? player : null;
                    emit.fire(mob.getWorld(), new DungeonEvent.MobDamaged(attacker, mob, mobId(mob), event.getFinalDamage(), max <= 0 ? 0 : after / max * 100));
                }));
        register(BukkitEventAdapter.custom("interact", PlayerInteractEvent.class).always().priority(EventPriority.HIGH)
                .emitsCancellable("block_interacted", BLOCK_POSITION).emits("redstone_activated", "x", "y", "z", "powered")
                .translate(BukkitEventAdapters::interact));
        register(BukkitEventAdapter.custom("block_break", BlockBreakEvent.class).always().priority(EventPriority.HIGH).ignoreCancelled()
                .emitsCancellable("block_broken", BLOCK_POSITION)
                .translate((event, emit) -> emit.fire(event.getBlock().getWorld(), new DungeonEvent.BlockBroken(event.getPlayer(), event.getBlock(), event))));
        register(BukkitEventAdapter.custom("block_place", BlockPlaceEvent.class).always().priority(EventPriority.HIGH).ignoreCancelled()
                .emitsCancellable("block_placed", BLOCK_POSITION)
                .translate((event, emit) -> {
                    Block block = event.getBlockPlaced();
                    if (isCustomPlacement(event)) return;
                    emit.fire(block.getWorld(), new DungeonEvent.BlockPlaced(event.getPlayer(), block, BlockRef.VANILLA_PREFIX + block.getType().getKey().getKey(), event));
                }));
        register(BukkitEventAdapter.custom("custom_block_place", SmpBlockPlaceEvent.class).always().priority(EventPriority.HIGH).ignoreCancelled()
                .emitsCancellable("block_placed", BLOCK_POSITION)
                .translate((event, emit) -> emit.fire(event.getTarget().getWorld(), new DungeonEvent.BlockPlaced(event.getPlayer(), event.getTarget(), event.getBlock().getId(), event))));
        register(BukkitEventAdapter.custom("pickup", EntityPickupItemEvent.class).always().ignoreCancelled()
                .emits("item_picked_up", "amount")
                .translate((event, emit) -> {
                    if (!(event.getEntity() instanceof Player player)) return;
                    ItemStack item = event.getItem().getItemStack();
                    emit.fire(player.getWorld(), new DungeonEvent.ItemPickedUp(player, item, ItemRef.idOf(item), item.getAmount()));
                }));
        register(BukkitEventAdapter.custom("portal", PlayerMoveEvent.class).always().ignoreCancelled()
                .emits("portal_entered")
                .translate(BukkitEventAdapters::portal));
        register(BukkitEventAdapter.custom("player_death", PlayerDeathEvent.class).always().priority(EventPriority.HIGH)
                .emits("player_died")
                .translate((event, emit) -> emit.fire(event.getPlayer().getWorld(), new DungeonEvent.PlayerDied(event.getPlayer(), event))));
        register(BukkitEventAdapter.custom("quest_completed", QuestCompletedEvent.class).always()
                .emits("quest_completed")
                .translate((event, emit) -> {
                    if (!(event.getHolder() instanceof GroupHolder group) || !group.owner().equals(QuestsFeature.OWNER)) return;
                    emit.fire(DungeonManager.getInstance().get(group.id()), new DungeonEvent.QuestCompleted(event.getQuest()));
                }));
    }

    private static void registerScriptEvents() {
        register(BukkitEventAdapter.of("player_fished", PlayerFishEvent.class)
                .player(PlayerFishEvent::getPlayer)
                .location(event -> event.getHook().getLocation())
                .subject(event -> event.getState().name().toLowerCase(Locale.ROOT))
                .data("exp", event -> DataValue.of(event.getExpToDrop()))
                .data("caught", event -> DataValue.of(event.getCaught() != null))
                .build());
        register(BukkitEventAdapter.of("item_dropped", PlayerDropItemEvent.class)
                .player(PlayerDropItemEvent::getPlayer)
                .subject(event -> ItemRef.idOf(event.getItemDrop().getItemStack()))
                .data("amount", event -> DataValue.of(event.getItemDrop().getItemStack().getAmount()))
                .build());
        register(BukkitEventAdapter.of("item_consumed", PlayerItemConsumeEvent.class)
                .player(PlayerItemConsumeEvent::getPlayer)
                .subject(event -> ItemRef.idOf(event.getItem()))
                .build());
        register(BukkitEventAdapter.of("player_sneaked", PlayerToggleSneakEvent.class)
                .player(PlayerToggleSneakEvent::getPlayer)
                .data("sneaking", event -> DataValue.of(event.isSneaking()))
                .build());
        register(BukkitEventAdapter.of("entity_interacted", PlayerInteractEntityEvent.class)
                .player(PlayerInteractEntityEvent::getPlayer)
                .location(event -> event.getRightClicked().getLocation())
                .subject(event -> entityId(event.getRightClicked()))
                .build());
        register(BukkitEventAdapter.of("ability_cast", AbilityCastEvent.class)
                .player(event -> event.getSmpPlayer().getBukkitPlayer())
                .subject(event -> event.getAbility().getId())
                .data("level", event -> DataValue.of(event.getAbility().getLevel()))
                .build());
    }

    // Pressure plates fire once when first stepped on; levers report their new state.
    private static void interact(PlayerInteractEvent event, BukkitEventAdapter.Emitter emit) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() == Action.PHYSICAL) {
            if (isPressurePlate(block) && !isPowered(block.getBlockData())) {
                emit.fire(block.getWorld(), new DungeonEvent.RedstoneActivated(event.getPlayer(), block, true));
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        emit.fire(block.getWorld(), new DungeonEvent.BlockInteracted(event.getPlayer(), block, event));
        if (event.useInteractedBlock() == Event.Result.DENY || !(block.getBlockData() instanceof Switch button)) return;
        boolean lever = block.getType() == Material.LEVER;
        if (!lever && button.isPowered()) return;
        emit.fire(block.getWorld(), new DungeonEvent.RedstoneActivated(event.getPlayer(), block, !lever || !button.isPowered()));
    }

    private static void portal(PlayerMoveEvent event, BukkitEventAdapter.Emitter emit) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        if (DungeonManager.getInstance().getByWorld(player.getWorld()) == null) {
            INSIDE_PORTAL.remove(player.getUniqueId());
            return;
        }
        Block portal = touchedPortal(player, event.getTo());
        if (portal == null) {
            INSIDE_PORTAL.remove(player.getUniqueId());
            return;
        }
        if (INSIDE_PORTAL.put(player.getUniqueId(), portal.getType()) == portal.getType()) return;
        emit.fire(player.getWorld(), new DungeonEvent.PortalEntered(player, portal.getType(), portal.getLocation()));
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

    private static boolean isPowered(BlockData data) {
        if (data instanceof AnaloguePowerable analogue) return analogue.getPower() > 0;
        return data instanceof Powerable powerable && powerable.isPowered();
    }

    private static boolean isPressurePlate(Block block) {
        return Tag.PRESSURE_PLATES.isTagged(block.getType()) || block.getType() == Material.TRIPWIRE;
    }

    private static boolean isCustomPlacement(BlockPlaceEvent event) {
        return event.getBlockPlaced().getType() == Material.BARRIER && ItemStackUtils.getBaseId(event.getItemInHand()) != null;
    }

    private static String mobId(LivingEntity mob) {
        BaseEntity base = EntityUtils.getBaseEntity(mob);
        return base != null ? base.getId() : mob.getType().getKey().getKey();
    }

    private static String entityId(Entity entity) {
        return entity instanceof LivingEntity living ? mobId(living) : entity.getType().getKey().getKey();
    }

    private static void register(BukkitEventAdapter<?> adapter) {
        Registries.DUNGEON_BUKKIT_EVENT.register(adapter.name(), adapter);
    }
}
