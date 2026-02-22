package com.roguesmp.effect;

import com.google.gson.*;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.registry.EffectCodecRegistry;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class EffectManager {
    public static final int PERIOD = 5;
    public static final String DATA_FOLDER = "player_effect_tmp";

    private static EffectManager INSTANCE;

    private final Map<UUID, Map<String, TreeSet<SmpEffect>>> allEffects = new HashMap<>();
    private final Map<UUID, Map<String, TreeSet<SmpEffect>>> playerCache = new HashMap<>();

    private final BukkitRunnable runnable;

    private EffectManager(JavaPlugin plugin) {
        registerCommand();

        runnable = new BukkitRunnable() {
            int mTicks = 0;

            @Override
            public void run() {

                mTicks += PERIOD;
                boolean fourHz = mTicks % 5 == 0;
                boolean twoHz = mTicks % 10 == 0;
                boolean oneHz = mTicks % 20 == 0;

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
                        currentActiveEffect.onTick(entity, oneHz, twoHz, fourHz);
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

    public void addEffect(Entity entity, String sourceId, SmpEffect smpEffect) {
        UUID uuid = entity.getUniqueId();
        // Get or create the inner map for this entity
        Map<String, TreeSet<SmpEffect>> entityEffects = allEffects.computeIfAbsent(uuid, k -> new HashMap<>());

        // Get or create the TreeSet for this effect type
        TreeSet<SmpEffect> effects = entityEffects.computeIfAbsent(sourceId, k -> new TreeSet<>());
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

    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Map<String, TreeSet<SmpEffect>> effectMap = playerCache.get(uuid);
        if (effectMap == null) {
            Utils.runAsync(() -> {
                Map<String, TreeSet<SmpEffect>> effects = loadPlayerEffectsFromFile(uuid);
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
        Map<String, TreeSet<SmpEffect>> effectMap = allEffects.get(le.getUniqueId());
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
        Map<String, TreeSet<SmpEffect>> effectMap = allEffects.get(le.getUniqueId());
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
        playerCache.put(uuid, allEffects.get(uuid));

        Utils.runLater(() -> {
            //Player logged in before removal is run
            if (Bukkit.getEntity(uuid) != null) return;
            Map<String, TreeSet<SmpEffect>> effectsMap = playerCache.remove(uuid);
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

    private static void savePlayerEffectToFile(UUID playerId, @NotNull Map<String, TreeSet<SmpEffect>> effectsMap) {
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
                    JsonElement element = gson.toJsonTree(effect);
                    if (!element.isJsonObject()) {
                        RogueSmpCore.LOGGER.warn("Effect {} failed serialization for player {}", effect.getEffectID(), playerId);
                        continue;
                    }
                    array.add(element.getAsJsonObject());
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

    private static @Nullable Map<String, TreeSet<SmpEffect>> loadPlayerEffectsFromFile(@NotNull UUID playerId) {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), DATA_FOLDER);
        File playerFile = new File(folder, playerId + ".json");

        if (!playerFile.exists()) {
            return null;
        }

        Map<String, TreeSet<SmpEffect>> result = new HashMap<>();

        try (Reader reader = new FileReader(playerFile)) {
            Gson gson = Utils.GSON;
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var entry : root.entrySet()) {
                String key = entry.getKey();
                JsonArray array = entry.getValue().getAsJsonArray();
                TreeSet<SmpEffect> effects = new TreeSet<>();

                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonElement idElement = obj.get("effectID");
                    if (idElement == null) {
                        RogueSmpCore.LOGGER.warn("Missing id in effect for player {}", playerId);
                        continue;
                    }

                    String id = idElement.getAsString();
                    Class<? extends SmpEffect> clazz = EffectCodecRegistry.get(id);
                    if (clazz == null) {
                        RogueSmpCore.LOGGER.warn("Unknown effect id '{}' for player {}", id, playerId);
                        continue;
                    }

                    SmpEffect effect = gson.fromJson(obj, clazz);
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

    private static void registerCommand() {
        new CommandAPICommand("smpeffect")
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new IntegerArgument("duration"), new DoubleArgument("magnitude"))
                        .executesPlayer((player, commandArguments) -> {
                            int duration = (Integer) commandArguments.get("duration");
                            double magnitude = (Double) commandArguments.get("magnitude");
                            EffectManager.getInstance().addEffect(player, "command", new DamageIncreaseEffect(duration, magnitude));
                        }))
                .register();
    }
}