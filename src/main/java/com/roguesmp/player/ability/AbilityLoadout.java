package com.roguesmp.player.ability;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AbilityLoadout {
    private final SmpPlayer smpPlayer;

    // Internal runtime storage: Fast array access per type
    private final Map<AbilityType, Ability[]> abilityMap = new EnumMap<>(AbilityType.class);

    // State Management for abilities that "intercept" inputs (e.g., aiming modes)
    private Ability contextOwner = null;
    private int contextTicksLeft = 0;

    public AbilityLoadout(SmpPlayer smpPlayer) {
        this.smpPlayer = smpPlayer;

        // Initialize arrays based on Enum definitions
        for (AbilityType type : AbilityType.values()) {
            abilityMap.put(type, new Ability[type.getMaxSlots()]);
        }
    }

    /**
     * Unified equip method.
     * Automatically syncs the runtime Ability instance and the PlayerData persistence.
     */
    public void equip(AbilityType type, @Nullable Ability ability, int index) {
        Ability[] slots = abilityMap.get(type);
        if (slots == null || index < 0 || index >= slots.length) return;
        if (ability != null && !isAllowedForCurrentClass(ability.getId())) return;

        slots[index] = ability;

        String id = (ability == null) ? null : ability.getId();
        smpPlayer.getPlayerData().setEquippedAbility(type, index, id);
    }

    /**
     * Whether the given ability belongs to the player's currently selected {@link PlayerClass} -
     * abilities can only be equipped while their owning class is active, though once unlocked
     * their level is permanent regardless of class switches (see {@link PlayerClass}).
     */
    public boolean isAllowedForCurrentClass(String abilityId) {
        String classId = smpPlayer.getPlayerData().getClassId();
        if (classId == null) return false;

        PlayerClass playerClass = Registries.PLAYER_CLASS.get(classId);
        return playerClass != null && playerClass.hasAbility(abilityId);
    }

    /**
     * Helper to check if an ability ID is already equipped in a specific category.
     */
    public boolean isEquipped(String abilityId, AbilityType type) {
        Ability[] slots = abilityMap.get(type);
        if (slots == null) return false;
        return Arrays.stream(slots).anyMatch(a -> a != null && a.getId().equals(abilityId));
    }

    public void loadData(PlayerData data) {
        Map<String, Integer> unlocked = data.getUnlockedAbilities();

        for (AbilityType type : AbilityType.values()) {
            List<String> equippedIds = data.getEquippedByType(type);

            for (int i = 0; i < equippedIds.size(); i++) {
                String id = equippedIds.get(i);
                if (id == null) continue;

                int level = unlocked.getOrDefault(id, 0);
                if (level <= 0) continue; // Not unlocked (e.g. unlocked ability was later revoked) - drop from the loadout

                AbilityInfo<?> info = Registries.ABILITY.get(id);
                if (info == null) continue;
                Ability ability = info.createInstance(smpPlayer, level);

                if (ability != null) {
                    // Update internal map only (to avoid redundant dirty-flag triggers in PlayerData)
                    abilityMap.get(type)[i] = ability;
                }
            }
        }
    }

    private int lastCastTick = 0;

    public void cast(AbilityTrigger.Key key) {
        int currentTick = Bukkit.getCurrentTick();
        if (currentTick - lastCastTick <= 2) return;
        lastCastTick = currentTick; //Only allow 1 key trigger per 2 tick
        // 1. Give Context Owner priority (Interceptor pattern)
        if (contextOwner != null) {
            if (execute(contextOwner, key)) return;
        }

        // 2. Check all Active abilities in order
        Ability[] actives = abilityMap.get(AbilityType.ACTIVE);
        for (Ability ability : actives) {
            if (ability == null || ability == contextOwner) continue;
            if (execute(ability, key)) return;
        }
    }

    // --- Execution Logic ---

    @SuppressWarnings("unchecked")
    private boolean execute(Ability ability, AbilityTrigger.Key key) {
        AbilityInfo<Ability> info = (AbilityInfo<Ability>) ability.getAbilityInfo();
        String actionKey = info.findMatchingActionKey(smpPlayer.getBukkitPlayer(), key);

        if (actionKey == null) return false;

        AbilityCastEvent event = new AbilityCastEvent(smpPlayer, ability);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;

        AbilityResponse resp = info.executeSpecificAction(ability, actionKey);
        return processSignal(ability, resp);
    }

    private boolean processSignal(Ability ability, AbilityResponse resp) {
        switch (resp.signal()) {
            case CAPTURE -> {
                this.contextOwner = ability;
                this.contextTicksLeft = resp.timeoutTicks();
                return true;
            }
            case RELEASE -> {
                this.contextOwner = null;
                this.contextTicksLeft = 0;
                return true;
            }
            case CONSUME -> { return true; }
            case CONTINUE -> { return false; }
            default -> { return false; }
        }
    }

    // --- Getters ---

    public Ability[] getAbilities(AbilityType type) {
        return abilityMap.getOrDefault(type, new Ability[0]);
    }

    public SmpPlayer getSmpPlayer() {
        return smpPlayer;
    }

    /**
     * A private helper to execute logic on every non-null ability
     * in the loadout, regardless of type.
     */
    private void forEachAbility(Consumer<Ability> action) {
        for (Ability[] abilities : abilityMap.values()) {
            for (Ability ability : abilities) {
                if (ability != null) {
                    action.accept(ability);
                }
            }
        }
    }

    public void tick(int periodIncrement) {
        // 1. Handle Input Context Timeout
        if (contextOwner != null) {
            contextTicksLeft -= periodIncrement;
            if (contextTicksLeft <= 0) contextOwner = null;
        }

        // 2. Tick all abilities across all categories
        forEachAbility(ability -> {
            // Handle Cooldowns
            if (ability.tickCooldown(PlayerManager.PERIOD)) {
                ability.onCooldownRefreshed();
            }
            // General Tick logic
            ability.tick(periodIncrement);
        });
    }

    // --- Event Listeners ---

    public void onDamageEntity(DamageEvent event) {
        forEachAbility(ability -> ability.onDamageEntity(event));
    }

    public void onKillEntity(EntityDeathEvent event) {
        forEachAbility(ability -> ability.onKillEntity(event));
    }

    public void onHurt(DamageEvent event) {
        forEachAbility(ability -> ability.onHurt(event));
    }

    public void onHurtFatal(DamageEvent event) {
        forEachAbility(ability -> ability.onHurtFatal(event));
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        forEachAbility(ability -> ability.onConsume(event));
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        forEachAbility(ability -> ability.onExpChange(event));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        forEachAbility(ability -> ability.onBlockBreak(event));
    }

    public void onCombust(EntityCombustEvent event) {
        forEachAbility(ability -> ability.onCombust(event));
    }

    public void onCombustEntity(EntityCombustByEntityEvent event) {
        forEachAbility(ability -> ability.onCombustEntity(event));
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        forEachAbility(ability -> ability.onProjectileHit(event));
    }

    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        forEachAbility(ability -> ability.onProjectileLaunch(event));
    }

    public void onShootArrow(EntityShootBowEvent event) {
        forEachAbility(ability -> ability.onShootArrow(event));
    }

    public void onConsumeArrow(ArrowConsumeEvent event) {
        forEachAbility(ability -> ability.onConsumeArrow(event));
    }

    public void onTeleport(PlayerTeleportEvent event) {
        forEachAbility(ability -> ability.onTeleport(event));
    }

    public void onDeath(PlayerDeathEvent event) {
        forEachAbility(ability -> ability.onDeath(event));
    }
}
