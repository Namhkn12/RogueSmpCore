package com.roguesmp.player;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.player.ability.AbilityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * A class that hold player-related data
 */
public class PlayerData{

    @GsonIgnore
    private boolean dirty = true;

    private final UUID uuid;
    private int level;
    private final Map<String, Integer> unlockedAbilities;

    private final Map<AbilityType, List<String>> equippedAbilities;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.unlockedAbilities = new HashMap<>();
        this.equippedAbilities = new EnumMap<>(AbilityType.class);

        // Initialize lists for each type to avoid null checks later
        for (AbilityType type : AbilityType.values()) {
            equippedAbilities.put(type, new ArrayList<>(Collections.nCopies(type.getMaxSlots(), null)));
        }
    }

    // Clone constructor
    public PlayerData(PlayerData other) {
        this.uuid = other.uuid;
        this.level = other.level;
        this.unlockedAbilities = new HashMap<>(other.unlockedAbilities);
        this.equippedAbilities = new EnumMap<>(AbilityType.class);

        other.equippedAbilities.forEach((type, list) ->
                this.equippedAbilities.put(type, new ArrayList<>(list))
        );

        this.dirty = other.dirty;
    }

    public void setEquippedAbility(AbilityType type, int index, @Nullable String abilityId) {
        List<String> list = equippedAbilities.get(type);
        if (list == null || index < 0 || index >= type.getMaxSlots()) return;

        list.set(index, abilityId);
        this.dirty = true;
    }

    public @Unmodifiable List<String> getEquippedByType(AbilityType type) {
        return Collections.unmodifiableList(equippedAbilities.getOrDefault(type, Collections.emptyList()));
    }

    // --- Legacy Compatibility Getters (Optional) ---

    public List<String> getActiveAbilities() { return getEquippedByType(AbilityType.ACTIVE); }
    public List<String> getPassiveAbilities() { return getEquippedByType(AbilityType.PASSIVE); }
    public @Nullable String getLifelineAbility() {
        List<String> lifeline = equippedAbilities.get(AbilityType.LIFELINE);
        return (lifeline != null && !lifeline.isEmpty()) ? lifeline.getFirst() : null;
    }

    public void setAbilityLevel(String abilityId, int level) {
        if (level <= 0) {
            unlockedAbilities.remove(abilityId);
        } else {
            unlockedAbilities.put(abilityId, level);
        }
        this.dirty = true;
    }

    public int getAbilityLevel(String abilityId) {
        return unlockedAbilities.getOrDefault(abilityId, 0);
    }

    public Map<String, Integer> getUnlockedAbilities() {
        return Collections.unmodifiableMap(unlockedAbilities);
    }

    public UUID getUuid() { return uuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; this.dirty = true; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
}
