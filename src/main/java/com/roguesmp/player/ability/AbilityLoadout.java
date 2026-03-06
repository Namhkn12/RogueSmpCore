package com.roguesmp.player.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.impl.GravityBomb;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class AbilityLoadout {
    public static final int MAX_PASSIVE_ABILITY = 5;

    private final SmpPlayer smpPlayer;
    private final Map<AbilityTrigger, Ability> equippedAbilities = new EnumMap<>(AbilityTrigger.class);
    private final Map<String, Ability> passiveAbilities = HashMap.newHashMap(MAX_PASSIVE_ABILITY);

    public AbilityLoadout(SmpPlayer smpPlayer) {
        this.smpPlayer = smpPlayer;

        equipActiveAbility(AbilityTrigger.SWAP, GravityBomb.INFO.getFactory().apply(smpPlayer, 2));
    }

    public boolean cast(AbilityTrigger trigger) {
        Ability ability = equippedAbilities.get(trigger);
        if (ability == null) return false;
        if (!ability.isOnCooldown()) {
            ability.cast();
            return true;
        }
        return true;
    }

    public void equipActiveAbility(AbilityTrigger trigger, Ability ability) {
        equippedAbilities.put(trigger, ability);
    }

    public void removeActiveAbility(AbilityTrigger trigger) {
        equippedAbilities.remove(trigger);
    }

    public void addPassiveAbility(String id, Ability ability) {
        passiveAbilities.put(id, ability);
    }

    public SmpPlayer getSmpPlayer() {
        return smpPlayer;
    }

    public Map<AbilityTrigger, Ability> getEquippedAbilities() {
        return equippedAbilities;
    }

    public Map<String, Ability> getPassiveAbilities() {
        return passiveAbilities;
    }

    public void tick(boolean twoHz, boolean oneHz) {
        equippedAbilities.forEach((trigger, ability) -> {
            if (ability.tickCooldown(PlayerManager.PERIOD)) {
                ability.onCooldownRefreshed();
            }
            ability.tick(twoHz, oneHz);
        });
        passiveAbilities.forEach((s, ability) -> {
            if (ability.tickCooldown(PlayerManager.PERIOD)) {
                ability.onCooldownRefreshed();
            }
            ability.tick(twoHz, oneHz);
        });
    }

    public void onDamageEntity(DamageEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onDamageEntity(event));
        passiveAbilities.forEach((s, ability) -> ability.onDamageEntity(event));
    }

    public void onKillEntity(EntityDeathEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onKillEntity(event));
        passiveAbilities.forEach((s, ability) -> ability.onKillEntity(event));
    }

    public void onHurt(DamageEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onHurt(event));
        passiveAbilities.forEach((s, ability) -> ability.onHurt(event));
    }

    public void onHurtFatal(DamageEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onHurtFatal(event));
        passiveAbilities.forEach((s, ability) -> ability.onHurtFatal(event));
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onConsume(event));
        passiveAbilities.forEach((s, ability) -> ability.onConsume(event));
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onExpChange(event));
        passiveAbilities.forEach((s, ability) -> ability.onExpChange(event));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onBlockBreak(event));
        passiveAbilities.forEach((s, ability) -> ability.onBlockBreak(event));
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onProjectileHit(event));
        passiveAbilities.forEach((s, ability) -> ability.onProjectileHit(event));
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        equippedAbilities.forEach((trigger, ability) -> ability.onProjectileLaunch(event));
        passiveAbilities.forEach((s, ability) -> ability.onProjectileLaunch(event));
    }
}
