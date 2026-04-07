package com.roguesmp.effect;

import com.google.gson.*;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.registry.EffectCodecRegistry;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
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
import java.util.concurrent.ConcurrentSkipListSet;

/*
 * The first map layer use UUID for identifying entities from each other.
 *
 * The second map layer uses String keys as "sources" of the effects, and ordered sets of effects as values.
 * Sources could be specific to an ability, like "PowerInjectionPercentSpeedEffect",
 * or more generic, like "VulnerabilityEffect" (increased damage, given by a variety of spells).
 *
 * Importantly, only the Effect with the highest "magnitude" from any given source is applied. Thus, all
 * spell specific buffs will stack with each other, while generic Vulnerability will only have the strongest
 * application take effect.
 *
 * Effects from the SAME SOURCE should ALWAYS be the SAME TYPE, and only ever have
 * differing durations and magnitudes.
 *
 * The ordered sets themselves are sorted by magnitude. While only the top Effect is ever applied, all Effects
 * are tracked and ticked down by the over-arching runnable, meaning that after a stronger Effect wears off,
 * longer lasting weaker Effects are still active and will be applied.
 */
public class EffectManager {
    public static final int PERIOD = 5;
    public static final String DATA_FOLDER = "player_effect_tmp";

    private static EffectManager INSTANCE;

    private final Map<UUID, Map<String, NavigableSet<SmpEffect>>> allEffects = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, NavigableSet<SmpEffect>>> playerCache = new ConcurrentHashMap<>();

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

                    var sourceEffects = entry.getValue();
                    var sourceIterator = sourceEffects.entrySet().iterator();

                    while (sourceIterator.hasNext()) {
                        var smpEffectsEntry = sourceIterator.next();
                        var effects = smpEffectsEntry.getValue();
                        if (effects.isEmpty()) {
                            sourceIterator.remove();
                            continue;
                        }

                        var effectIterator = effects.descendingIterator();
                        SmpEffect currentActiveEffect = effects.getLast(); //Only last effect (highest magnitude) is active
                        currentActiveEffect.onTick(entity, oneHz, twoHz);
                        boolean currentEffectRemoved = false;
                        while (effectIterator.hasNext()) {
                            SmpEffect effect = effectIterator.next();
                            if (currentEffectRemoved) {
                                currentActiveEffect.onGainEffect(entity);
                                currentEffectRemoved = false;
                            }

                            boolean tickResult = effect.tickDuration(PERIOD);
                            if (tickResult) {
                                if (currentActiveEffect == effect) { //If the expired effect is currently active effect
                                    //The entity could be dead after tickEffect was called
                                    if (entity.isValid() && !entity.isDead()) effect.onLoseEffect(entity);
                                    currentEffectRemoved = true;
                                }

                                effectIterator.remove(); //Remove expired effect

                                if (!effects.isEmpty()) { //If there's still pending effect, set it as currently active effect
                                    currentActiveEffect = effects.getLast();
                                }
                            }
                        }
                    }
                }
            }
        };

        runnable.runTaskTimer(plugin, 0, PERIOD);
    }

    public static void registerCommand() {
        new CommandAPICommand("smpeffect")
                .withSubcommand(new CommandAPICommand("add")
                        .withSubcommand(DamageIncreaseEffect.registerCommand())
                        .withSubcommand(SpeedEffect.registerCommand()))

                .register();
    }

    public void addEffect(Entity entity, String sourceId, SmpEffect smpEffect) {
        UUID uuid = entity.getUniqueId();
        // Get or create the inner map for this entity
        Map<String, NavigableSet<SmpEffect>> entityEffects = allEffects.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // Get or create the NavigableSet for this effect type
        NavigableSet<SmpEffect> effects = entityEffects.computeIfAbsent(sourceId, k -> new ConcurrentSkipListSet<>());
        if (!effects.isEmpty()) {
            SmpEffect currentActiveEffect = effects.getLast();
            // Iterate through effects to check if there is already an effect with existing magnitude but less duration.
            boolean foundEffect = false;
            for (SmpEffect effect : effects) {
                if (effect.compareTo(smpEffect) == 0 && effect.getDuration() < smpEffect.getDuration() && effect.getDeathBehavior() == smpEffect.getDeathBehavior()) {
                    //Update its duration
                    if (effect == currentActiveEffect) {
                        effect.onLoseEffect(entity);
                        effect.onGainEffect(entity);
                    }
                    effect.setDuration(smpEffect.getDuration());
                    foundEffect = true;
                    break;
                }
            }

            if (!foundEffect) {
                effects.add(smpEffect);
            }

            if (effects.getLast() == smpEffect) {
                currentActiveEffect.onLoseEffect(entity);
                smpEffect.onGainEffect(entity);
            }
        } else {
            effects.add(smpEffect);
            smpEffect.onGainEffect(entity);
        }

    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public Map<String, SmpEffect> getActiveEffects(Entity entity) {
        Map<String, NavigableSet<SmpEffect>> effects = allEffects.get(entity.getUniqueId());
        HashMap<String, SmpEffect> output = new HashMap<>();
        if (effects != null) {
            for (Map.Entry<String, NavigableSet<SmpEffect>> entry : effects.entrySet()) {
                try {
                    SmpEffect effect = entry.getValue().last();
                    if (effect != null) {
                        output.put(entry.getKey(), effect);
                    }
                } catch (NoSuchElementException e) {
                    // ignore - effect was probably removed in another thread (and this method can be called by the tab list from arbitrary threads)
                }
            }
        }
        return output;
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Map<String, NavigableSet<SmpEffect>> effectMap = playerCache.get(uuid);
        if (effectMap == null) {
            Utils.runAsync(() -> {
                Map<String, NavigableSet<SmpEffect>> effects = loadPlayerEffectsFromFile(uuid);
                if (effects != null) {
                    Utils.runLater(() -> {
                        allEffects.put(uuid, effects);
                        effects.forEach((s, smpEffects) -> {
                            smpEffects.getLast().onLoseEffect(event.getPlayer());
                            smpEffects.getLast().onGainEffect(event.getPlayer());
                        });
                        RogueSmpCore.LOGGER.info("Loaded effect for {}", event.getPlayer().getName());
                    });
                }
            });
            return;
        }

        allEffects.put(uuid, effectMap);

        effectMap.forEach((s, smpEffects) -> {
            SmpEffect effect = smpEffects.getLast();
            effect.onLoseEffect(event.getPlayer());
            effect.onGainEffect(event.getPlayer());
        });

        playerCache.remove(uuid);

    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        cachePlayerAndScheduleRemoval(event.getPlayer());
    }

    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity le = event.getEntity();
        Map<String, NavigableSet<SmpEffect>> effectMap = allEffects.get(le.getUniqueId());
        if (effectMap == null) return;
        if (le instanceof Player) {
            effectMap.forEach((s, smpEffects) -> {
                smpEffects.getLast().onDeath(event);
                var effectIter = smpEffects.descendingIterator();
                while (effectIter.hasNext()) {
                    SmpEffect smpEffect = effectIter.next();
                    if (smpEffect.getDeathBehavior() == SmpEffect.DeathBehavior.HALVES_ON_DEATH) {
                        smpEffect.setDuration(smpEffect.getDuration() / 2);
                    } else if (smpEffect.getDeathBehavior() == SmpEffect.DeathBehavior.REMOVE_ON_DEATH) {
                        effectIter.remove();
                    }
                }
            });
        } else {
            effectMap.forEach((s, smpEffects) -> {
                smpEffects.getLast().onDeath(event);
            });
            allEffects.remove(le.getUniqueId());
        }
    }

    public void onDamage(DamageEvent event) {
        Entity le = event.getDamager();
        if (le == null) return;
        Map<String, NavigableSet<SmpEffect>> effectMap = allEffects.get(le.getUniqueId());
        if (effectMap == null) return;
        effectMap.forEach((s, smpEffects) -> {
            smpEffects.getLast().onDamage(event);
        });
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new EffectManager(plugin);
    }

    public static EffectManager getInstance() {
        if (INSTANCE == null) {
            throw new NullPointerException("EffectManager is null!");
        }
        return INSTANCE;
    }

    private void cachePlayerAndScheduleRemoval(Player player) {
        UUID uuid = player.getUniqueId();
        var effects = allEffects.get(uuid);
        if (effects == null) return;
        playerCache.put(uuid, effects);

        Utils.runLater(() -> {
            //Player logged in before removal is run
            if (Bukkit.getEntity(uuid) != null) return;
            Map<String, NavigableSet<SmpEffect>> effectsMap = playerCache.remove(uuid);
            if (effectsMap != null) {
                effectsMap.forEach((s, smpEffects) -> {
                    var effectIter = smpEffects.descendingIterator();
                    while (effectIter.hasNext()) {
                        SmpEffect effect = effectIter.next();
                        if (!effect.isPersistent()) effectIter.remove();
                    }
                });
                Utils.runAsync(() -> {
                    if (effectsMap.isEmpty()) return;
                    savePlayerEffectToFile(uuid, effectsMap);
                });
            }
        }, 100); //Remove after 5s
    }

    private static void savePlayerEffectToFile(UUID playerId, @NotNull Map<String, NavigableSet<SmpEffect>> effectsMap) {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), DATA_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File playerFile = new File(folder, playerId.toString() + ".json");
        try (Writer writer = new FileWriter(playerFile)) {
            Gson gson = Utils.GSON;
            JsonObject root = new JsonObject();
            for (var entry : effectsMap.entrySet()) {
                String key = entry.getKey();
                JsonArray array = new JsonArray();

                for (SmpEffect effect : entry.getValue()) {
                    if (effect == null) continue;
                    // Convert effect directly to JsonObject
                    JsonObject object = effect.serialize();
                    object.addProperty("id", effect.getEffectID());
                    array.add(object);
                }
                root.add(key, array);
            }
            gson.toJson(root, writer);
            RogueSmpCore.LOGGER.info("Saved effects for {}", playerId);

        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("FAILED TO SAVE EFFECT FOR {}", playerId);
            e.printStackTrace();
        }
    }

    private static @Nullable Map<String, NavigableSet<SmpEffect>> loadPlayerEffectsFromFile(@NotNull UUID playerId) {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), DATA_FOLDER);
        File playerFile = new File(folder, playerId + ".json");

        if (!playerFile.exists()) {
            return null;
        }

        Map<String, NavigableSet<SmpEffect>> result = new HashMap<>();

        try (Reader reader = new FileReader(playerFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : root.entrySet()) {
                String key = entry.getKey();
                JsonArray array = entry.getValue().getAsJsonArray();
                NavigableSet<SmpEffect> effects = new ConcurrentSkipListSet<>();

                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonElement idElement = obj.get("id");
                    if (idElement == null) {
                        RogueSmpCore.LOGGER.warn("Missing id in effect for player {}", playerId);
                        continue;
                    }

                    String id = idElement.getAsString();
                    EffectCodecRegistry.EffectDeserializer effectDeserializer = EffectCodecRegistry.get(id);
                    if (effectDeserializer == null) {
                        RogueSmpCore.LOGGER.warn("Effect id '{}' has no serializer, for player {}", id, playerId);
                        continue;
                    }
                    SmpEffect effect = effectDeserializer.deserialize(obj);
                    effects.add(effect);
                }
                result.put(key, effects);
                playerFile.delete();
            }

        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("FAILED TO LOAD EFFECTS FOR {}", playerId);
            e.printStackTrace();
        }

        return result;
    }
}