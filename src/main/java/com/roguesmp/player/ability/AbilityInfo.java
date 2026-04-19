package com.roguesmp.player.ability;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.ability.trigger.InputSignal;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiFunction;

/**
 * This class hold static information of an ability (for use in Registry, creating ability instance, description, etc...)
 */
public class AbilityInfo<T extends Ability> {

    @FunctionalInterface
    public interface AbilityAction<T extends Ability> {
        AbilityResponse execute(T ability);
    }

    private final String id;
    private final Class<T> abilityClass;
    private final BiFunction<SmpPlayer, Integer, T> factory;
    private final List<String> description = new ArrayList<>();
    private String displayName;
    private Material icon;
    private AbilityType type;

    // Maps "prime" -> PredatorStrike::onPrime
    private final Map<String, AbilityAction<T>> actionRegistry = new HashMap<>();

    // Maps AbilityTrigger -> action key (Filled during JSON loading)
    private final Map<AbilityTrigger, String> triggerMap = new LinkedHashMap<>();

    // Key: attribute name (e.g., "damage"), Value: List of attribute values for levels [lvl1, lvl2, lvl3]
    private final Map<String, List<Double>> scaling = new HashMap<>();
    private final Map<Integer, List<UpgradeRequirement>> upgradeRequirement = new HashMap<>();
    private boolean loaded = false;

    public AbilityInfo(String id, Class<T> abilityClass, BiFunction<SmpPlayer, Integer, T> factory) {
        this.id = id;
        this.abilityClass = abilityClass;
        this.factory = factory;
    }

    public AbilityInfo<T> registerAction(String key, AbilityAction<T> action) {
        actionRegistry.put(key, action);
        return this;
    }

    public void bindTrigger(String actionKey, AbilityTrigger trigger) {
        this.triggerMap.put(trigger, actionKey);
    }

    // Part 1: Just find if a trigger matches and return the action key
    public String findMatchingActionKey(Player player, AbilityTrigger.Key pressed) {
        for (var entry : triggerMap.entrySet()) {
            if (entry.getKey().matches(player, pressed)) {
                // Return the first action key (or logic for list-based if needed)
                return entry.getValue();
            }
        }
        return null;
    }

    // Part 2: Actually run the code
    public AbilityResponse executeSpecificAction(T instance, String actionKey) {
        AbilityAction<T> action = actionRegistry.get(actionKey);
        if (action != null) {
            return action.execute(instance);
        }
        return AbilityResponse.continueChain();
    }

    public void populate(List<String> description,
                         Map<String, List<Double>> scaling,
                         String displayName,
                         Material icon,
                         AbilityType type,
                         Map<AbilityTrigger, String> triggerMap,
                         Map<Integer, List<UpgradeRequirement>> upgradeRequirement) {
        this.description.addAll(description);
        this.scaling.putAll(scaling);
        this.displayName = displayName;
        this.icon = icon;
        this.triggerMap.putAll(triggerMap);
        this.upgradeRequirement.putAll(upgradeRequirement);
        this.type = type;

        this.loaded = true;
    }

    public List<Component> getFormattedDescription(int level) {
        List<TagResolver> resolvers = new ArrayList<>();

        for (String key : scaling.keySet()) {
            double value = getAttributeForLevel(key, level);

            if (key.startsWith("cooldown")) { //Special handling for cooldown/duration tick formating
                String formattedCooldown = Utils.formatDecimal(value / 20);
                resolvers.add(Placeholder.parsed(key, formattedCooldown));
                continue;
            }
            if (key.startsWith("duration")) {
                String formattedDuration = Utils.formatDecimal(value / 20);
                resolvers.add(Placeholder.parsed(key, formattedDuration));
                continue;
            }
            String formattedValue = Utils.formatDecimal(value);
            // This maps <key> to the value (e.g., <dmg> -> 10.5)
            resolvers.add(Placeholder.parsed(key, formattedValue));
        }

        // Add any "Global" placeholders if needed (like the level itself)
        resolvers.add(Placeholder.parsed("level", String.valueOf(level)));

        List<Component> result = new ArrayList<>();
        for (String line : description) {
            result.add(MiniMessage.miniMessage().deserialize(line, TagResolver.resolver(resolvers)));
        }
        return result;
    }

    public Component getFormattedDisplayName() {
        return Utils.fromString(displayName);
    }

    /**
     * Level start at 1
     */
    public double getAttributeForLevel(String attr, int level) {
        List<Double> values = scaling.get(attr);
        if (values == null || values.isEmpty()) return 0.0;
        int index = Math.min(Math.max(0, level - 1), values.size() - 1);
        return values.get(index);
    }

    public String getId() {
        return id;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Class<T> getAbilityClass() {
        return abilityClass;
    }

    public BiFunction<SmpPlayer, Integer, T> getFactory() {
        return factory;
    }

    public AbilityType getType() {
        return type;
    }

    public Material getIcon() {
        return icon;
    }

    public @Unmodifiable Map<String, List<Double>> getScaling() {
        return Collections.unmodifiableMap(scaling);
    }

    public @Unmodifiable Map<String, AbilityAction<T>> getActionRegistry() {
        return Collections.unmodifiableMap(actionRegistry);
    }

    public @Unmodifiable Map<AbilityTrigger, String> getTriggerMap() {
        return Collections.unmodifiableMap(triggerMap);
    }

    public @Unmodifiable Map<Integer, List<UpgradeRequirement>> getUpgradeRequirement() {
        return Collections.unmodifiableMap(upgradeRequirement);
    }

    public boolean isLoaded() {
        return loaded;
    }
}
