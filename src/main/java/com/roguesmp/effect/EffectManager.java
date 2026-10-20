package com.roguesmp.effect;

import com.google.gson.*;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * The first map layer uses UUID for identifying entities from each other.
 *
 * The second layer, EntityEffects, holds one EffectStack per "source" of the effects. Sources
 * could be specific to an ability, like "PowerInjectionPercentSpeedEffect", or more generic, like
 * "VulnerabilityEffect" (increased damage, given by a variety of spells).
 *
 * Importantly, only the Effect with the highest "magnitude" from any given source is applied. Thus, all
 * spell specific buffs will stack with each other, while generic Vulnerability will only have the strongest
 * application take effect.
 *
 * Effects from the SAME SOURCE should ALWAYS be the SAME TYPE, and only ever have
 * differing durations and magnitudes.
 *
 * Each EffectStack tracks and ticks down every effect it holds, not just the active one, meaning
 * that after a stronger Effect wears off, longer lasting weaker Effects are still active and will
 * be applied.
 */
public class EffectManager {
    public static final int PERIOD = 5;
    private static final String DB_FILE_NAME = "player_effect_data.db";

    private static EffectManager INSTANCE;

    private final Map<UUID, EntityEffects> allEffects = new ConcurrentHashMap<>();
    private final Map<UUID, EntityEffects> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<PotionEffectType, Map<String, Integer>>> vanillaRequests = new ConcurrentHashMap<>();

    private final BukkitRunnable runnable;
    private final Connection connection;

    private EffectManager(JavaPlugin plugin) {
        this.connection = openConnection(plugin);
        createTable();

        runnable = new BukkitRunnable() {
            int mTicks = 0;

            @Override
            public void run() {

                mTicks += PERIOD;
                boolean twoHz = mTicks % 10 == 0;
                boolean oneHz = mTicks % 20 == 0;
                if (mTicks >= 20) mTicks = 0;

                var entryIterator = allEffects.entrySet().iterator();
                while (entryIterator.hasNext()) {
                    var entry = entryIterator.next();
                    UUID uuid = entry.getKey();
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity instanceof Player player) {
                        // A player on the death screen reads as dead/invalid, but their KEEP/HALVES_ON_DEATH
                        // effects must survive until respawn. Death is handled by onPlayerDeath.
                        if (player.isValid() && !player.isDead()) entry.getValue().tick(player, PERIOD, oneHz, twoHz);
                        continue;
                    }
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        // Entity still resolvable but gone (removed/despawned without a death event) - unwind its effects.
                        if (entity != null) entry.getValue().removeAll(entity);
                        entryIterator.remove();
                        continue;
                    }

                    entry.getValue().tick(entity, PERIOD, oneHz, twoHz);
                }
            }
        };

        runnable.runTaskTimer(plugin, 0, PERIOD);
    }

    private Connection openConnection(JavaPlugin plugin) {
        File dbFile = new File(plugin.getDataFolder(), DB_FILE_NAME);
        File parent = dbFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open player effect data database", e);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_effect_data (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    data BLOB NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create player_effect_data table", e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to close player effect data database", e);
        }
    }

    public void addEffect(Entity entity, String sourceId, SmpEffect smpEffect) {
        // e.g. an ability's damage killed it before its effect was applied - nothing would ever clean this up
        if (entity.isDead()) return;
        EntityEffects entityEffects = allEffects.computeIfAbsent(entity.getUniqueId(), k -> new EntityEffects());
        entityEffects.getOrCreateStack(sourceId).add(entity, smpEffect);
    }

    public @Nullable EffectStack clearEffects(Entity entity, String sourceId) {
        EntityEffects entityEffects = allEffects.get(entity.getUniqueId());
        if (entityEffects == null) return null;
        return entityEffects.removeEffects(entity, sourceId);
    }

    public Map<String, SmpEffect> getActiveEffects(Entity entity) {
        EntityEffects entityEffects = allEffects.get(entity.getUniqueId());
        return entityEffects == null ? Map.of() : entityEffects.activeEffects();
    }

    /**
     * Asks for a vanilla potion effect on {@code entity} under {@code source} - the entity always
     * carries the strongest amplifier among all active requests, with infinite duration, since
     * timing belongs to the requesting {@link SmpEffect}.
     */
    public void requestVanilla(Entity entity, String source, PotionEffectType type, int amplifier) {
        vanillaRequests.computeIfAbsent(entity.getUniqueId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(source, amplifier);
        syncVanilla(entity, type);
    }

    public void releaseVanilla(Entity entity, String source, PotionEffectType type) {
        Map<PotionEffectType, Map<String, Integer>> byType = vanillaRequests.get(entity.getUniqueId());
        if (byType != null) {
            Map<String, Integer> requests = byType.get(type);
            if (requests != null) {
                requests.remove(source);
                if (requests.isEmpty()) byType.remove(type);
            }
            if (byType.isEmpty()) vanillaRequests.remove(entity.getUniqueId());
        }
        syncVanilla(entity, type);
    }

    private void syncVanilla(Entity entity, PotionEffectType type) {
        if (!(entity instanceof LivingEntity living)) return;

        Map<PotionEffectType, Map<String, Integer>> byType = vanillaRequests.get(entity.getUniqueId());
        Map<String, Integer> requests = byType == null ? null : byType.get(type);
        PotionEffect current = living.getPotionEffect(type);

        if (requests == null || requests.isEmpty()) {
            if (current != null) living.removePotionEffect(type);
            return;
        }

        int amplifier = Collections.max(requests.values());
        if (current != null && current.isInfinite() && current.getAmplifier() == amplifier) return;
        if (current != null) living.removePotionEffect(type);
        living.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, true, true));
    }

    /**
     * Every vanilla potion effect change not made by plugin is absorbed into the effect system:
     * additions become {@link VanillaPotionEffect}s, removals are refused (so milk does nothing),
     * except {@code /effect clear}, which clears the vanilla-sourced stack instead. Expiry passes
     * through - managed effects are infinite, so it only ends timed effects other code applied.
     */
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN || event.getCause() == EntityPotionEffectEvent.Cause.EXPIRATION) return;

        Entity entity = event.getEntity();
        PotionEffectType type = event.getModifiedType();
        switch (event.getAction()) {
            case ADDED, CHANGED -> {
                PotionEffect added = event.getNewEffect();
                if (added == null || type.isInstant()) return;

                event.setCancelled(true);
                int duration = added.isInfinite() ? SmpEffect.INFINITE : added.getDuration();
                Utils.runLater(() -> {
                    if (entity.isValid()) addEffect(entity, VanillaPotionEffect.sourceOf(type), new VanillaPotionEffect(duration, type, added.getAmplifier()));
                });
            }
            case REMOVED, CLEARED -> {
                event.setCancelled(true);
                if (event.getCause() == EntityPotionEffectEvent.Cause.COMMAND) {
                    Utils.runLater(() -> clearEffects(entity, VanillaPotionEffect.sourceOf(type)));
                }
            }
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        vanillaRequests.remove(uuid);
        for (PotionEffect stale : event.getPlayer().getActivePotionEffects()) {
            if (stale.isInfinite()) event.getPlayer().removePotionEffect(stale.getType());
        }
        EntityEffects cached = playerCache.remove(uuid);
        if (cached != null) {
            allEffects.put(uuid, cached);
            cached.refresh(event.getPlayer());
            return;
        }

        Utils.runAsync(() -> {
            EntityEffects loaded = loadPlayerEffectData(uuid);
            if (loaded != null) {
                Utils.runLater(() -> {
                    allEffects.put(uuid, loaded);
                    loaded.refresh(event.getPlayer());
                    RogueSmpCore.LOGGER.info("Loaded effect for {}", event.getPlayer().getName());
                });
            }
        });
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        vanillaRequests.remove(event.getPlayer().getUniqueId());
        cachePlayerAndScheduleRemoval(event.getPlayer());
    }

    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity le = event.getEntity();
        EntityEffects entityEffects = allEffects.get(le.getUniqueId());
        if (entityEffects == null) return;
        if (le instanceof Player) {
            entityEffects.onPlayerDeath(event);
        } else {
            entityEffects.onNonPlayerDeath(event);
            entityEffects.removeAll(le);
            allEffects.remove(le.getUniqueId());
        }
    }

    public void handleDamageEvent(DamageEvent event) {
        Entity entity = event.getDamager();
        if (entity != null) {
            UUID sourceUuid = null;
            if (entity instanceof EvokerFangs fangs) {
                if (fangs.getOwner() != null) sourceUuid = fangs.getOwner().getUniqueId();
            } else {
                sourceUuid = entity.getUniqueId();
            }

            if (sourceUuid != null) {
                EntityEffects entityEffects = allEffects.get(sourceUuid);
                if (entityEffects != null) entityEffects.onDamageEntity(event);
            }
        }

        Entity victim = event.getVictim();
        if (victim != null) {
            EntityEffects entityEffects = allEffects.get(victim.getUniqueId());
            if (entityEffects != null) entityEffects.onHurt(event);
        }
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new EffectManager(plugin);
    }

    public static EffectManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("EffectManager is null!");
        }
        return INSTANCE;
    }

    private void cachePlayerAndScheduleRemoval(Player player) {
        UUID uuid = player.getUniqueId();
        EntityEffects entityEffects = allEffects.get(uuid);
        if (entityEffects == null) return;
        playerCache.put(uuid, entityEffects);

        Utils.runLater(() -> {
            //Player logged in before removal is run
            if (Bukkit.getEntity(uuid) != null) return;
            EntityEffects cached = playerCache.remove(uuid);
            if (cached != null) {
                cached.removeNonPersistent();
                Utils.runAsync(() -> {
                    savePlayerEffectData(uuid, cached);
                });
            }
        }, 100); //Remove after 5s
    }

    private synchronized void savePlayerEffectData(UUID playerId, @NotNull EntityEffects entityEffects) {
        JsonObject root = new JsonObject();
        for (var entry : entityEffects.snapshot().entrySet()) {
            String key = entry.getKey();
            List<SmpEffect> effects = entry.getValue().stream().filter(Objects::nonNull).toList();

            DataResult<JsonElement> encoded = Codec.listOf(SmpEffect.CODEC).encode(effects, JsonOps.INSTANCE);
            if (!encoded.isSuccess()) {
                RogueSmpCore.LOGGER.warn("Failed to encode effects for {} (source '{}'): {}", playerId, key, encoded.error());
                continue;
            }
            root.add(key, encoded.result());
        }

        String sql = """
                INSERT INTO player_effect_data (uuid, data)
                VALUES (?, jsonb(?))
                ON CONFLICT(uuid) DO UPDATE SET data = excluded.data
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, Utils.GSON.toJson(root));
            statement.executeUpdate();
            RogueSmpCore.LOGGER.info("Saved effects for {}", playerId);
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("FAILED TO SAVE EFFECT FOR {}", playerId, e);
        }
    }

    private synchronized @Nullable EntityEffects loadPlayerEffectData(@NotNull UUID playerId) {
        String selectSql = "SELECT json(data) AS data FROM player_effect_data WHERE uuid = ?";
        Map<String, List<SmpEffect>> result = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, playerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                JsonObject root = JsonParser.parseString(resultSet.getString("data")).getAsJsonObject();
                Codec<List<SmpEffect>> listCodec = Codec.lenientListOf(SmpEffect.CODEC,
                        (index, error) -> RogueSmpCore.LOGGER.warn("Skipped invalid effect [{}] for {}: {}", index, playerId, error));

                for (var entry : root.entrySet()) {
                    String key = entry.getKey();
                    DataResult<List<SmpEffect>> decoded = listCodec.decode(entry.getValue(), JsonOps.INSTANCE);
                    if (decoded.isSuccess()) {
                        result.put(key, decoded.result());
                    } else {
                        RogueSmpCore.LOGGER.warn("Failed to decode effects for {} (source '{}'): {}", playerId, key, decoded.error());
                        result.put(key, List.of());
                    }
                }
            }
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("FAILED TO LOAD EFFECTS FOR {}", playerId, e);
            return null;
        }

        // This data only exists to bridge a relog - once read back, it's consumed.
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_effect_data WHERE uuid = ?")) {
            delete.setString(1, playerId.toString());
            delete.executeUpdate();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("FAILED TO DELETE CONSUMED EFFECT DATA FOR {}", playerId, e);
        }

        return EntityEffects.fromSnapshot(result);
    }
}
