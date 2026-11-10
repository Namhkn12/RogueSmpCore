package com.roguesmp.worldboss;

import com.roguesmp.GlobalConfig;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.worldboss.definition.PeriodicSpawn;
import com.roguesmp.worldboss.definition.SummonedSpawn;
import com.roguesmp.worldboss.definition.WorldBossDefinition;
import com.roguesmp.worldboss.encounter.WorldBossEncounter;
import com.roguesmp.worldboss.runtime.ArenaState;
import com.roguesmp.worldboss.runtime.RemovalReason;
import com.roguesmp.worldboss.runtime.FightResult;
import com.roguesmp.worldboss.runtime.OpenResult;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class WorldBossManager {

    private static WorldBossManager INSTANCE;
    private static final long PERIODIC_CHECK_TICKS = 20L * 20;

    private final RogueSmpCore plugin;
    private final Map<String, WorldBossInstance> active = new HashMap<>();
    private final Map<String, Long> cooldownUntil = new HashMap<>();
    private final Map<String, Long> nextSpawnAt = new HashMap<>();
    private final Map<UUID, WorldBossDefinition> pendingRespawns = new HashMap<>();
    private @Nullable BukkitTask periodicTask;

    private WorldBossManager(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public OpenResult open(WorldBossDefinition definition, @Nullable UUID summoner, boolean force) {
        if (active.containsKey(definition.id())) return OpenResult.ALREADY_ACTIVE;
        if (!force && isDisabled(definition.id())) return OpenResult.DISABLED;
        if (!force && getCooldownRemaining(definition.id()) > 0) return OpenResult.ON_COOLDOWN;

        Supplier<? extends WorldBossEncounter> encounter = Registries.WORLD_BOSS_ENCOUNTER.get(definition.id());
        if (encounter == null) {
            RogueSmpCore.LOGGER.error("World boss '{}' has no registered encounter", definition.id());
            return OpenResult.UNKNOWN_ENCOUNTER;
        }

        Location center = definition.location().toLocation();
        if (center == null) return OpenResult.WORLD_NOT_LOADED;

        ServerRegionManager regions = ServerRegionManager.getInstance();
        List<ProtectedRegion> arena = regions.pieces(center.getWorld(), definition.arena());
        if (arena.isEmpty()) {
            RogueSmpCore.LOGGER.error("World boss '{}' references unknown arena region '{}'", definition.id(), definition.arena());
            return OpenResult.REGION_NOT_FOUND;
        }

        WorldBossInstance instance = new WorldBossInstance(definition, encounter.get(),
                center, arena, regions.displayName(center.getWorld(), definition.arena()), summoner,
                definition.spawn().joinWindowSeconds());
        active.put(definition.id(), instance);
        instance.getEncounter().onOpen(instance);
        instance.setTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(instance), 20L, 20L));
        return OpenResult.OPENED;
    }

    private void tick(WorldBossInstance instance) {
        int secondsLeft = instance.getSecondsLeft() - 1;
        instance.setSecondsLeft(secondsLeft);

        if (instance.getState() == ArenaState.OPEN) {
            if (secondsLeft > 0) {
                showCountdown(instance, secondsLeft);
                instance.getEncounter().onCountdown(instance, secondsLeft);
            } else {
                lock(instance);
            }
            return;
        }

        SmpEntity boss = instance.getBoss();
        if (boss == null || boss.getEntity().isDead()) {
            endFight(instance, FightResult.VICTORY);
            return;
        }
        if (!boss.getEntity().isValid()) {
            endFight(instance, FightResult.CANCELED);
            return;
        }

        for (UUID uuid : instance.getAliveParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) eliminate(instance, uuid, null, RemovalReason.LOGOUT);
            else if (!instance.contains(player.getLocation())) eliminate(instance, uuid, player, RemovalReason.LEFT_ARENA);
        }
        if (active.get(instance.getDefinition().id()) != instance) return;

        if (secondsLeft <= 0) {
            endFight(instance, FightResult.TIMEOUT);
            return;
        }
        String color = secondsLeft <= 60 ? "<red>" : "<yellow>";
        showActionBar(instance, instance.getDefinition().displayName() + " <gray>| Thời gian còn lại: "
                + color + String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60));
    }

    private void showCountdown(WorldBossInstance instance, int secondsLeft) {
        showActionBar(instance, instance.getDefinition().displayName() + " <gray>xuất hiện sau <yellow>" + secondsLeft + " giây");
    }

    private void showActionBar(WorldBossInstance instance, String miniMessage) {
        Component message = Utils.fromString(miniMessage);
        instance.getCenter().getWorld().getPlayers().stream()
                .filter(player -> instance.contains(player.getLocation()))
                .forEach(player -> player.sendActionBar(message));
    }

    private void admitOrBoot(WorldBossInstance instance, Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer != null && instance.getDefinition().meetsRequirements(smpPlayer)) {
            instance.addParticipant(player.getUniqueId());
            return;
        }
        player.teleport(exitFor(instance.getDefinition(), player), PlayerTeleportEvent.TeleportCause.PLUGIN);
        instance.getEncounter().onRemove(instance, player, RemovalReason.REQUIREMENT_NOT_MET);
    }

    private void lock(WorldBossInstance instance) {
        instance.setState(ArenaState.LOCKED);
        instance.getCenter().getWorld().getPlayers().stream()
                .filter(player -> !player.isDead())
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                .filter(player -> instance.contains(player.getLocation()))
                .forEach(player -> admitOrBoot(instance, player));

        if (instance.getParticipants().size() < instance.getDefinition().minParticipants()) {
            endFight(instance, FightResult.CANCELED);
            return;
        }

        SmpEntity boss = instance.getEncounter().spawnBoss(instance);
        if (boss == null) {
            RogueSmpCore.LOGGER.error("World boss '{}' failed to spawn its boss", instance.getDefinition().id());
            endFight(instance, FightResult.CANCELED);
            return;
        }
        instance.setBoss(boss);
        instance.setSecondsLeft(instance.getDefinition().fightTimeoutSeconds());
        markStarted(instance.getDefinition());
        instance.getEncounter().onLock(instance);
    }

    private void markStarted(WorldBossDefinition definition) {
        long now = System.currentTimeMillis();
        switch (definition.spawn()) {
            case SummonedSpawn summoned -> cooldownUntil.put(definition.id(), now + summoned.cooldownSeconds() * 1000L);
            case PeriodicSpawn periodic -> nextSpawnAt.put(definition.id(), now + rollIntervalSeconds(periodic) * 1000L);
        }
    }

    private long rollIntervalSeconds(PeriodicSpawn periodic) {
        int jitter = periodic.jitterSeconds();
        int offset = jitter <= 0 ? 0 : ThreadLocalRandom.current().nextInt(-jitter, jitter + 1);
        return Math.max(0, periodic.intervalSeconds() + offset);
    }

    public void eliminate(Player player, RemovalReason reason) {
        WorldBossInstance instance = getFightOf(player.getUniqueId());
        if (instance != null) eliminate(instance, player.getUniqueId(), player, reason);
    }

    private void eliminate(WorldBossInstance instance, UUID uuid, @Nullable Player player, RemovalReason reason) {
        if (active.get(instance.getDefinition().id()) != instance) return;
        if (instance.getState() != ArenaState.LOCKED || !instance.eliminate(uuid)) return;
        if (reason == RemovalReason.DEATH) pendingRespawns.put(uuid, instance.getDefinition());
        if (player != null) instance.getEncounter().onRemove(instance, player, reason);
        if (instance.getAliveParticipants().isEmpty()) endFight(instance, FightResult.WIPE);
    }

    public Location exitFor(WorldBossDefinition definition, Player player) {
        Location exit = definition.exit().resolve(player);
        return exit != null ? exit : player.getWorld().getSpawnLocation();
    }

    public @Nullable Location takePendingRespawn(Player player) {
        WorldBossDefinition definition = pendingRespawns.remove(player.getUniqueId());
        return definition == null ? null : exitFor(definition, player);
    }

    public void handleEntityDeath(LivingEntity entity) {
        active.values().stream()
                .filter(instance -> instance.isBoss(entity.getUniqueId()))
                .findFirst()
                .ifPresent(instance -> endFight(instance, FightResult.VICTORY));
    }

    public boolean cancel(String bossId) {
        WorldBossInstance instance = active.get(bossId);
        if (instance == null) return false;
        endFight(instance, FightResult.CANCELED);
        return true;
    }

    private void endFight(WorldBossInstance instance, FightResult result) {
        if (!active.remove(instance.getDefinition().id(), instance)) return;
        if (instance.getTask() != null) instance.getTask().cancel();

        SmpEntity boss = instance.getBoss();
        if (boss != null && result != FightResult.VICTORY && boss.getEntity().isValid()) boss.getEntity().remove();

        switch (result) {
            case VICTORY -> instance.getEncounter().onVictory(instance, instance.getAlivePlayers());
            case WIPE -> instance.getEncounter().onWipe(instance);
            case TIMEOUT -> instance.getEncounter().onTimeout(instance);
            case CANCELED -> instance.getEncounter().onCancel(instance);
        }
    }

    public void grantRewards(WorldBossInstance instance) {
        if (!instance.markRewarded()) return;
        instance.getAlivePlayers().forEach(player -> reward(instance, player));
    }

    private void reward(WorldBossInstance instance, Player player) {
        WorldBossDefinition definition = instance.getDefinition();
        LootTable table = Registries.LOOT_TABLE.get(definition.lootTable());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (table == null || smpPlayer == null) {
            RogueSmpCore.LOGGER.warn("World boss '{}' could not reward {} (loot table '{}')", definition.id(), player.getName(), definition.lootTable());
            return;
        }
        List<ItemStack> items = table.roll(LootContext.builder(smpPlayer).build());
        PlayerUtils.giveItem(player, items);
    }

    public @Nullable WorldBossInstance getBlockingFight(Player player, Location location) {
        for (WorldBossInstance instance : active.values()) {
            if (instance.getState() == ArenaState.LOCKED) {
                if (!instance.isAlive(player.getUniqueId())) {
                    if (instance.contains(location)) {
                        return instance;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable WorldBossInstance getFightOf(UUID uuid) {
        for (WorldBossInstance instance : active.values()) {
            if (instance.isAlive(uuid)) {
                return instance;
            }
        }
        return null;
    }

    public @Nullable WorldBossInstance getActive(String bossId) {
        return active.get(bossId);
    }

    public Collection<WorldBossInstance> getActiveInstances() {
        return Collections.unmodifiableCollection(active.values());
    }

    public boolean isDisabled(String bossId) {
        return RogueSmpCore.getGlobalConfig().isWorldBossDisabled(bossId);
    }

    public void setDisabled(String bossId, boolean disabled) {
        GlobalConfig config = RogueSmpCore.getGlobalConfig();
        config.setWorldBossDisabled(bossId, disabled);
        Utils.runAsync(() -> config.save(plugin));
    }

    public long getCooldownRemaining(String bossId) {
        return Math.max(0, cooldownUntil.getOrDefault(bossId, 0L) - System.currentTimeMillis());
    }

    public @Nullable Long getNextSpawnAt(String bossId) {
        return nextSpawnAt.get(bossId);
    }

    public void start() {
        periodicTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPeriodicSpawns, PERIODIC_CHECK_TICKS, PERIODIC_CHECK_TICKS);
    }

    private void checkPeriodicSpawns() {
        long now = System.currentTimeMillis();
        for (WorldBossDefinition definition : Registries.WORLD_BOSS.getAll().values()) {
            if (!(definition.spawn() instanceof PeriodicSpawn periodic)) continue;

            Long spawnAt = nextSpawnAt.get(definition.id());
            if (spawnAt == null) {
                nextSpawnAt.put(definition.id(), now + rollIntervalSeconds(periodic) * 1000L);
                continue;
            }
            if (now < spawnAt) continue;
            if (Bukkit.getOnlinePlayers().size() < periodic.minOnlinePlayers()) continue;

            open(definition, null, false);
        }
    }

    public void shutdown() {
        if (periodicTask != null) periodicTask.cancel();
        List.copyOf(active.values()).forEach(instance -> endFight(instance, FightResult.CANCELED));
    }

    public void validate() {
        for (WorldBossDefinition definition : Registries.WORLD_BOSS.getAll().values()) {
            if (Registries.WORLD_BOSS_ENCOUNTER.get(definition.id()) == null) {
                RogueSmpCore.LOGGER.error("World boss '{}' has no registered encounter", definition.id());
            }
        }
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new WorldBossManager(plugin);
    }

    public static WorldBossManager getInstance() {
        return INSTANCE;
    }
}
