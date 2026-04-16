package com.roguesmp.registry.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.player.ability.impl.active.*;
import com.roguesmp.player.ability.impl.lifeline.LastBreath;
import com.roguesmp.player.ability.impl.passive.Dodging;
import com.roguesmp.player.ability.impl.passive.Sharpshooter;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AbilityRegistry {

    public static String FOLDER_NAME = "ability_info";
    private static AbilityRegistry INSTANCE;

    private final Map<String, AbilityInfo<? extends Ability>> registry = new LinkedHashMap<>();

    private AbilityRegistry() {
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

    public @Nullable Ability createInstance(String id, SmpPlayer smpPlayer, int level) {
        AbilityInfo<?> info = registry.get(id);
        if (info == null) return null;
        return info.getFactory().apply(smpPlayer, level);
    }

    public@Nullable AbilityInfo<? extends Ability> getInfo(String id) {
        return registry.get(id);
    }

    public @Unmodifiable Collection<AbilityInfo<?>> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public @Unmodifiable List<AbilityInfo<?>> getByType(AbilityType type) {
        List<AbilityInfo<?>> infos = new ArrayList<>();
        registry.values().forEach(abilityInfo -> {
            if (abilityInfo.getType() == type) infos.add(abilityInfo);
        });
        return Collections.unmodifiableList(infos);
    }

    /**
     * Loops through the directory and populates registered AbilityInfo objects.
     */
    public void loadAll() {

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
                JsonObject json = Utils.GSON.fromJson(reader, JsonObject.class);

                // Use the filename (minus .json) or an "id" field in the JSON to find the Info object
                String id = json.has("id") ? json.get("id").getAsString() :
                        file.getName().replace(".json", "");

                AbilityInfo<?> info = registry.get(id);

                if (info != null) {
                    this.load(json, info);
                    count++;
                } else {
                    // Log a warning if a JSON exists but no INFO is registered for it
                    RogueSmpCore.LOGGER.warn("No AbilityInfo registered for ID: {}", id);
                }
            } catch (Exception e) {
                RogueSmpCore.LOGGER.warn("Failed to load ability file: {}", file.getName());
                e.printStackTrace();
            }
        }
        RogueSmpCore.LOGGER.info("Successfully loaded {} abilities from JSON.", count);
    }

    private void load(JsonObject json, AbilityInfo<?> info) {
        String displayName = getString(json, "display_name", info.getId());

        JsonArray descriptionsArray = json.has("description") ? json.getAsJsonArray("description") : new JsonArray();
        List<String> descriptions = new ArrayList<>();
        descriptionsArray.forEach(jsonElement -> descriptions.add(jsonElement.getAsString()));

        Material icon = Material.valueOf(getString(json, "icon", "BARRIER").toUpperCase());

        AbilityType type = AbilityType.valueOf(getString(json, "type", "PASSIVE").toUpperCase());

        Map<String, List<Double>> scaling = parseScaling(json);

        Map<AbilityTrigger, String> triggerMap = parseTriggers(json);

        Map<Integer, List<UpgradeRequirement>> upgrades = parseUpgrades(json);

        info.populate(descriptions, scaling, displayName, icon, type, triggerMap, upgrades);
    }

    /**
     * Parses the "scaling" section: "damage": [1.0, 2.0, 3.0]
     */
    private Map<String, List<Double>> parseScaling(JsonObject json) {
        Map<String, List<Double>> scalingMap = new HashMap<>();
        if (!json.has("scaling")) return scalingMap;

        JsonObject obj = json.getAsJsonObject("scaling");
        for (String key : obj.keySet()) {
            List<Double> values = new ArrayList<>();
            JsonArray array = obj.getAsJsonArray(key);
            for (JsonElement e : array) {
                values.add(e.getAsDouble());
            }
            scalingMap.put(key, values);
        }
        return scalingMap;
    }

    /**
     * Parses the "trigger" section into the unique AbilityTrigger objects
     */
    private Map<AbilityTrigger, String> parseTriggers(JsonObject json) {
        Map<AbilityTrigger, String> map = new LinkedHashMap<>();
        if (!json.has("trigger")) return map;

        JsonObject obj = json.getAsJsonObject("trigger");
        for (String actionKey : obj.keySet()) {
            JsonObject data = obj.getAsJsonObject(actionKey);

            // Physical Key
            AbilityTrigger.Key key = AbilityTrigger.Key.valueOf(
                    data.get("key").getAsString().toUpperCase()
            );

            // Options (Strings that map to Predicates in the Registry)
            List<String> options = new ArrayList<>();
            if (data.has("options")) {
                data.getAsJsonArray("options").forEach(opt -> options.add(opt.getAsString()));
            }

            // Create unique trigger and map to action key
            map.put(new AbilityTrigger(key, options), actionKey);
        }
        return map;
    }

    /**
     * Uses the UpgradeRequirementRegistry to create polymorphic requirements
     */
    private Map<Integer, List<UpgradeRequirement>> parseUpgrades(JsonObject json) {
        Map<Integer, List<UpgradeRequirement>> upgradeMap = new HashMap<>();
        if (!json.has("upgrades")) return upgradeMap;

        JsonObject obj = json.getAsJsonObject("upgrades");
        for (String levelStr : obj.keySet()) {
            int level = Integer.parseInt(levelStr);
            List<UpgradeRequirement> requirements = new ArrayList<>();
            JsonArray array = obj.getAsJsonArray(levelStr);

            for (JsonElement element : array) {
                JsonObject reqObj = element.getAsJsonObject();
                String type = reqObj.get("type").getAsString();

                // Call the registry factory we built in the previous step
                UpgradeRequirement req = UpgradeRequirementRegistry.create(type, reqObj);
                requirements.add(req);
            }
            upgradeMap.put(level, requirements);
        }
        return upgradeMap;
    }

    // Helper for safe string retrieval
    private String getString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    public static void init() {
        INSTANCE = new AbilityRegistry();
    }

    public static AbilityRegistry getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AbilityRegistry is null!");
        }
        return INSTANCE;
    }

    private void register(AbilityInfo<? extends Ability> info) {
        registry.put(info.getId(), info);
    }
}
