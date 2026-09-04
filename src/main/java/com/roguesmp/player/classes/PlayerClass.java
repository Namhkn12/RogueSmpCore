package com.roguesmp.player.classes;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable JSON-defined template for a player class (e.g. "warrior", "mage"). Owned by
 * {@link com.roguesmp.registry.Registries#PLAYER_CLASS}.
 * <p>
 * {@link #defaultAbilities} doubles as this class's full ability roster: it's both what gets
 * granted (at the given level) the first time a player picks this class, and the set of ability
 * ids a player is allowed to equip while this class is active - see
 * {@link com.roguesmp.player.ability.AbilityLoadout#isAllowedForCurrentClass(String)}. A level of
 * {@code 0} means "in this class's roster but not auto-granted" -
 * {@link com.roguesmp.player.SmpPlayer#setPlayerClass} won't unlock it for free, so it shows as
 * locked in {@link com.roguesmp.gui.ability.AbilityCatalogue} until the player unlocks it
 * themselves, while still being equippable (once unlocked) like any other ability in the roster.
 */
public class PlayerClass {

    public static final Codec<PlayerClass> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(PlayerClass::getId),
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(PlayerClass::getDisplayName),
            Codec.MATERIAL.optionalFieldOf("icon", Material.BARRIER).forGetter(PlayerClass::getIcon),
            Codec.listOf(Codec.STRING).optionalFieldOf("description", List.of()).forGetter(PlayerClass::getDescription),
            Codec.lenientUnboundedMap(Codec.INT).optionalFieldOf("default_abilities", Map.of()).forGetter(PlayerClass::getDefaultAbilities),
            Codec.listOf(Codec.STRING).optionalFieldOf("allowedWeapons", List.of())
                    .forGetter(playerClass -> List.copyOf(playerClass.allowedWeapons)),
            PlayerClass::new
    );

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final Map<String, Integer> defaultAbilities;
    private final Set<String> allowedWeapons;

    public PlayerClass(String id, String displayName, Material icon, List<String> description, Map<String, Integer> defaultAbilities, List<String> allowedWeapons) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.defaultAbilities = defaultAbilities;
        this.allowedWeapons = new HashSet<>(allowedWeapons);
    }

    public boolean hasAbility(String abilityId) {
        return defaultAbilities.containsKey(abilityId);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName.isEmpty() ? id : displayName;
    }

    public Component getFormattedDisplayName() {
        return Utils.fromString(getDisplayName());
    }

    public Material getIcon() {
        return icon;
    }

    public @Unmodifiable List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public @Unmodifiable Map<String, Integer> getDefaultAbilities() {
        return Collections.unmodifiableMap(defaultAbilities);
    }

    /**
     * @return ids of {@link com.roguesmp.tag.SmpTag}s (from {@link com.roguesmp.registry.Registries#ITEM}'s
     * tags) whose members this class may wield - not raw item ids.
     */
    public @Unmodifiable Set<String> getAllowedWeapons() {
        return Collections.unmodifiableSet(allowedWeapons);
    }
}
