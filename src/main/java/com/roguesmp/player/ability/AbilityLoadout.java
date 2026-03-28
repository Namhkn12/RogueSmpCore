package com.roguesmp.player.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.AbilityRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class AbilityLoadout {
    public static final int MAX_PASSIVE_ABILITY = 14;

    private final SmpPlayer smpPlayer;
    private final Map<AbilityTrigger, Ability> equippedAbilities = new EnumMap<>(AbilityTrigger.class);
    private final List<Ability> passiveAbilities = new ArrayList<>();

    public AbilityLoadout(SmpPlayer smpPlayer) {
        this.smpPlayer = smpPlayer;

//        equipActiveAbility(AbilityTrigger.SWAP, GravityBomb.INFO.getFactory().apply(smpPlayer, 2));
    }

    public boolean cast(AbilityTrigger trigger) {
        Ability ability = equippedAbilities.get(trigger);
        if (ability == null) return false;
        if (!ability.isOnCooldown()) {
            smpPlayer.getBukkitPlayer().sendActionBar(Component.text("Kích hoạt kĩ năng ", NamedTextColor.YELLOW).append(ability.getAbilityInfo().displayText()));
            ability.cast();
            Bukkit.getPluginManager().callEvent(new AbilityCastEvent(smpPlayer, ability));
            return true;
        }
        return false;
    }

    public void equipActive(AbilityTrigger trigger, Ability ability) {
        equippedAbilities.put(trigger, ability);
    }

    public void removeActive(AbilityTrigger trigger) {
        equippedAbilities.remove(trigger);
    }

    public void equipPassive(Ability ability) {
        for (Ability ability1 : passiveAbilities) {
            // If already equipped, ignore
            if (ability1.getAbilityInfo().id().equals(ability.getAbilityInfo().id())) return;
        }
        passiveAbilities.add(ability);
    }

    public void removePassive(String id) {
        passiveAbilities.removeIf(a -> a.getAbilityInfo().id().equals(id));
    }

    public void loadData(PlayerData data) {
        Map<String, Integer> pairs = data.getUnlockedAbilities();

        Map<AbilityTrigger, String> equippedIds = data.getEquippedAbilities();
        equippedIds.forEach((trigger, s) -> {
            int level = pairs.getOrDefault(s, 1);
            Ability ability = AbilityRegistry.createInstance(s, smpPlayer, level);
            if (ability != null) {
                equipActive(trigger, ability);
            }
        });

        List<String> passives = data.getPassiveAbilities();
        passives.forEach(s -> {
            int level = pairs.getOrDefault(s, 1);
            Ability ability = AbilityRegistry.createInstance(s, smpPlayer, level);
            if (ability != null) {
                equipPassive(ability);
            }
        });
    }

    public SmpPlayer getSmpPlayer() {
        return smpPlayer;
    }

    public Map<AbilityTrigger, Ability> getActiveAbilities() {
        return equippedAbilities;
    }

    public List<Ability> getPassiveAbilities() {
        return passiveAbilities;
    }

    /**
     * Return a list of all equipped abilities, with passive first and active last. The order of active is the same as AbilityTrigger enum
     * @return An unmodifiable list containing all equipped abilities
     */
    public @Unmodifiable List<Ability> getAbilities() {
        List<Ability> abilities = new ArrayList<>(passiveAbilities);
        abilities.addAll(equippedAbilities.values());
        return List.copyOf(abilities);
    }

    public void tick(int periodIncrement) {
        passiveAbilities.forEach((ability) -> {
            if (ability.tickCooldown(PlayerManager.PERIOD)) {
                ability.onCooldownRefreshed();
            }
            ability.tick(periodIncrement);
        });
        equippedAbilities.forEach((trigger, ability) -> {
            if (ability.tickCooldown(PlayerManager.PERIOD)) {
                ability.onCooldownRefreshed();
            }
            ability.tick(periodIncrement);
        });
    }

    public void onDamageEntity(DamageEvent event) {
        passiveAbilities.forEach((ability) -> ability.onDamageEntity(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onDamageEntity(event));
    }

    public void onKillEntity(EntityDeathEvent event) {
        passiveAbilities.forEach((ability) -> ability.onKillEntity(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onKillEntity(event));
    }

    public void onHurt(DamageEvent event) {
        passiveAbilities.forEach((ability) -> ability.onHurt(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onHurt(event));
    }

    public void onHurtFatal(DamageEvent event) {
        passiveAbilities.forEach((ability) -> ability.onHurtFatal(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onHurtFatal(event));
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        passiveAbilities.forEach((ability) -> ability.onConsume(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onConsume(event));
    }

    public void onExpChange(PlayerExpChangeEvent event) {
        passiveAbilities.forEach((ability) -> ability.onExpChange(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onExpChange(event));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        passiveAbilities.forEach((ability) -> ability.onBlockBreak(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onBlockBreak(event));
    }

    public void onCombust(EntityCombustEvent event) {
        passiveAbilities.forEach((ability) -> ability.onCombust(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onCombust(event));
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        passiveAbilities.forEach((ability) -> ability.onProjectileHit(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onProjectileHit(event));
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        passiveAbilities.forEach((ability) -> ability.onProjectileLaunch(event));
        equippedAbilities.forEach((trigger, ability) -> ability.onProjectileLaunch(event));
    }
}
