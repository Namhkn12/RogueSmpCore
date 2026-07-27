package com.roguesmp.registry.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityConfig;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.player.ability.impl.active.*;
import com.roguesmp.player.ability.impl.lifeline.LastBreath;
import com.roguesmp.player.ability.impl.passive.Dodging;
import com.roguesmp.player.ability.impl.passive.Sharpshooter;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hardcodes every {@link AbilityInfo} into {@link Registries#ABILITY} (behavior/factory), then
 * overlays tunable data (description/scaling/trigger/upgrades) onto them from
 * {@code ability_info/*.json} via {@link #loadAll()} — same hybrid code+JSON pattern as
 * {@code EntityRegistry}. See {@link AbilityConfig} for the JSON shape.
 */
public class AbilityInfoRegistry {

    public static final String FOLDER_NAME = "ability_info";

    public static void bootstrap() {
        register(InfernalOverdrive.INFO);
        register(AetherStance.INFO);
        register(IgneousRune.INFO);
        register(Scrapshot.INFO);
        register(Sidearm.INFO);
        register(Fireball.INFO);

        register(FireworkBlast.INFO);
        register(Flamestrike.INFO);

        register(GravityBomb.INFO);
        register(FlameSpirit.INFO);

        register(VolcanicMeteor.INFO);
        register(Raygun.INFO);
        register(Pyroblast.INFO);
        //Lifeline
        register(LastBreath.INFO);
        // Passive
        register(Dodging.INFO);
        register(Sharpshooter.INFO);
    }

    private static void register(AbilityInfo<? extends Ability> info) {
        Registries.ABILITY.register(info.getId(), info);
    }

    public static @Nullable Ability createInstance(String id, SmpPlayer smpPlayer, int level) {
        AbilityInfo<?> info = Registries.ABILITY.get(id);
        if (info == null) return null;
        return info.getFactory().apply(smpPlayer, level);
    }

    public static @Unmodifiable List<AbilityInfo<?>> getByType(AbilityType type) {
        List<AbilityInfo<?>> infos = new ArrayList<>();
        Registries.ABILITY.getAll().values().forEach(info -> {
            if (info.getType() == type) infos.add(info);
        });
        return List.copyOf(infos);
    }

    /**
     * Loops through the directory and populates registered AbilityInfo objects.
     */
    public static void loadAll() {
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER_NAME);

        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int count = 0;
        for (File file : files) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
                if (json == null) continue;

                // Use the filename (minus .json) or an "id" field in the JSON to find the Info object
                JsonObject root = json.getAsJsonObject();
                String id = root.has("id") ? root.get("id").getAsString() :
                        file.getName().replace(".json", "");

                AbilityInfo<?> info = Registries.ABILITY.get(id);
                if (info == null) {
                    // Log a warning if a JSON exists but no INFO is registered for it
                    RogueSmpCore.LOGGER.warn("No AbilityInfo registered for ID: {}", id);
                    continue;
                }

                DataResult<AbilityConfig> result = AbilityConfig.CODEC.decode(json, JsonOps.INSTANCE);
                if (!result.isSuccess()) {
                    RogueSmpCore.LOGGER.warn("Failed to decode ability config [{}] in '{}': {}", id, FOLDER_NAME, result.error());
                    continue;
                }

                load(result.result(), info);
                count++;
            } catch (Exception e) {
                RogueSmpCore.LOGGER.warn("Failed to load ability file: {}", file.getName());
                e.printStackTrace();
            }
        }
        RogueSmpCore.LOGGER.info("Successfully loaded {} abilities from JSON.", count);
    }

    private static void load(AbilityConfig config, AbilityInfo<?> info) {
        String displayName = config.displayName().isEmpty() ? info.getId() : config.displayName();

        // JSON authors triggers keyed by action ("prime": {key: ..., options: [...]}); AbilityInfo
        // wants the inverse (trigger -> action key), since matching goes trigger-first.
        Map<AbilityTrigger, String> triggerMap = new LinkedHashMap<>();
        config.triggers().forEach((actionKey, trigger) -> triggerMap.put(trigger, actionKey));

        info.populate(config.description(), config.scaling(), displayName, config.icon(), config.type(), triggerMap, config.upgrades());
    }
}
