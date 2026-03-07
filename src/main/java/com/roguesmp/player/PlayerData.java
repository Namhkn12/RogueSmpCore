package com.roguesmp.player;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.AbilityTrigger;

import java.util.*;

/**
 * A class that hold player-related data
 */
public class PlayerData {

    @GsonIgnore
    private boolean dirty = true;

    private final UUID uuid;
    private int level;
    private Map<String, Integer> unlockedAbilities = new HashMap<>();
    private Map<AbilityTrigger, String> equippedAbilities = new EnumMap<>(AbilityTrigger.class);
    private List<String> passiveAbilities = new ArrayList<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<String, Integer> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    public void setUnlockedAbilities(Map<String, Integer> unlockedAbilities) {
        this.unlockedAbilities = unlockedAbilities;
    }

    public List<String> getPassiveAbilities() {
        return passiveAbilities;
    }

    public void setPassiveAbilities(List<String> passiveAbilities) {
        this.passiveAbilities = passiveAbilities;
    }

    public Map<AbilityTrigger, String> getEquippedAbilities() {
        return equippedAbilities;
    }

    public void setEquippedAbilities(Map<AbilityTrigger, String> equippedAbilities) {
        this.equippedAbilities = equippedAbilities;
    }
}
