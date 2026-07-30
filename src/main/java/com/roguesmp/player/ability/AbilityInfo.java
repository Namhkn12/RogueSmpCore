package com.roguesmp.player.ability;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.registry.Registries;
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
 * Hardcoded half of an ability: identity, instance factory, and click-action bindings - none of
 * which can come from JSON (they're Java code). Tunable data (description/scaling/trigger/upgrades)
 * lives separately in {@link AbilityConfig}, looked up by {@link #getId()} from
 * {@link Registries#ABILITY_CONFIG} at call time.
 */
public class AbilityInfo<T extends Ability> {

    @FunctionalInterface
    public interface AbilityAction<T extends Ability> {
        AbilityResponse execute(T ability);
    }

    private final String id;
    private final Class<T> abilityClass;
    private final BiFunction<SmpPlayer, Integer, T> factory;

    // Maps "prime" -> PredatorStrike::onPrime
    private final Map<String, AbilityAction<T>> actionRegistry = new HashMap<>();

    public AbilityInfo(String id, Class<T> abilityClass, BiFunction<SmpPlayer, Integer, T> factory) {
        this.id = id;
        this.abilityClass = abilityClass;
        this.factory = factory;
    }

    public AbilityInfo<T> registerAction(String key, AbilityAction<T> action) {
        actionRegistry.put(key, action);
        return this;
    }

    public T createInstance(SmpPlayer smpPlayer, int level) {
        return factory.apply(smpPlayer, level);
    }

    private AbilityConfig config() {
        return Registries.ABILITY_CONFIG.getOrDefault(id, AbilityConfig.DEFAULT_CONFIG);
    }

    // Part 1: Just find if a trigger matches and return the action key
    public String findMatchingActionKey(Player player, AbilityTrigger.Key pressed) {
        for (var entry : config().triggers().entrySet()) {
            if (entry.getValue().matches(player, pressed)) {
                return entry.getKey();
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

    public List<Component> getFormattedDescription(int level) {
        Map<String, List<Double>> scaling = getScaling();
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
        for (String line : config().description()) {
            result.add(MiniMessage.miniMessage().deserialize(line, TagResolver.resolver(resolvers)));
        }
        return result;
    }

    public Component getFormattedDisplayName() {
        return Utils.fromString(getDisplayName());
    }

    /**
     * Level start at 1
     */
    public double getAttributeForLevel(String attr, int level) {
        List<Double> values = getScaling().get(attr);
        if (values == null || values.isEmpty()) return 0.0;
        int index = Math.min(Math.max(0, level - 1), values.size() - 1);
        return values.get(index);
    }

    public boolean hasNextLevel(int nextLevel) {
        return getScaling().values().stream().anyMatch(list -> list.size() >= nextLevel + 1);
    }

    public String getId() {
        return id;
    }

    public @Unmodifiable List<String> getDescription() {
        return Collections.unmodifiableList(config().description());
    }

    public String getDisplayName() {
        String displayName = config().displayName();
        return displayName.isEmpty() ? id : displayName;
    }

    public Class<T> getAbilityClass() {
        return abilityClass;
    }

    public AbilityType getType() {
        return config().type();
    }

    public Material getIcon() {
        return config().icon();
    }

    public @Unmodifiable Map<String, List<Double>> getScaling() {
        return Collections.unmodifiableMap(config().scaling());
    }

    public @Unmodifiable Map<String, AbilityAction<T>> getActionRegistry() {
        return Collections.unmodifiableMap(actionRegistry);
    }

    public @Unmodifiable Map<Integer, List<UpgradeRequirement>> getUpgradeRequirement() {
        return Collections.unmodifiableMap(config().upgrades());
    }
}
