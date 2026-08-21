package com.roguesmp.effect;

import com.google.gson.*;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.effect.impl.BleedingEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.PotentPoisonEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
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
    public static final String DATA_FOLDER = "player_effect_tmp";

    private static EffectManager INSTANCE;

    private final Map<UUID, EntityEffects> allEffects = new ConcurrentHashMap<>();
    private final Map<UUID, EntityEffects> playerCache = new ConcurrentHashMap<>();

    private final BukkitRunnable runnable;

    private EffectManager(JavaPlugin plugin) {

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
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        entryIterator.remove();
                        continue;
                    }

                    entry.getValue().tick(entity, PERIOD, oneHz, twoHz);
                }
            }
        };

        runnable.runTaskTimer(plugin, 0, PERIOD);
    }

    public static void registerCommand() {
        new CommandAPICommand("smpeffect")
                .withSubcommand(new CommandAPICommand("add")
                        .withSubcommand(DamageIncreaseEffect.registerCommand())
                        .withSubcommand(SpeedEffect.registerCommand())
                        .withSubcommand(BleedingEffect.registerCommand())
                        .withSubcommand(PotentPoisonEffect.registerCommand()))

                .register();
    }

    public void addEffect(Entity entity, String sourceId, SmpEffect smpEffect) {
        EntityEffects entityEffects = allEffects.computeIfAbsent(entity.getUniqueId(), k -> new EntityEffects());
        entityEffects.getOrCreateStack(sourceId).add(entity, smpEffect);
    }

    public Map<String, SmpEffect> getActiveEffects(Entity entity) {
        EntityEffects entityEffects = allEffects.get(entity.getUniqueId());
        return entityEffects == null ? Map.of() : entityEffects.activeEffects();
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        EntityEffects cached = playerCache.remove(uuid);
        if (cached != null) {
            allEffects.put(uuid, cached);
            cached.refresh(event.getPlayer());
            return;
        }

        Utils.runAsync(() -> {
            EntityEffects loaded = loadPlayerEffectsFromFile(uuid);
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
                    if (cached.isEmpty()) return;
                    savePlayerEffectToFile(uuid, cached);
                });
            }
        }, 100); //Remove after 5s
    }

    private static void savePlayerEffectToFile(UUID playerId, @NotNull EntityEffects entityEffects) {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File playerFile = new File(folder, playerId + ".json");
        try (Writer writer = new FileWriter(playerFile)) {
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
            Utils.GSON.toJson(root, writer);
            RogueSmpCore.LOGGER.info("Saved effects for {}", playerId);

        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("FAILED TO SAVE EFFECT FOR {}", playerId);
            e.printStackTrace();
        }
    }

    private static @Nullable EntityEffects loadPlayerEffectsFromFile(@NotNull UUID playerId) {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), DATA_FOLDER);
        File playerFile = new File(folder, playerId + ".json");

        if (!playerFile.exists()) {
            return null;
        }

        Map<String, List<SmpEffect>> result = new HashMap<>();

        try (Reader reader = new FileReader(playerFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
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

            playerFile.delete();
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("FAILED TO LOAD EFFECTS FOR {}", playerId);
            e.printStackTrace();
        }

        return EntityEffects.fromSnapshot(result);
    }
}
