package com.roguesmp.event;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom damage event for more control
 */
public class DamageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity victim;
    private final @Nullable Entity damager;
    private final double initialDamage;
    private final DamageType damageType;

    private boolean needUpdate = false; // For recalculating dmg value

    private boolean ignoreIframe;
    private boolean isCancelled;
    private boolean isCritical;

    // ===== DAMAGE NUMERIC BUCKETS =====
    private double baseOverride = -1;
    private double addBase = 0;
    private double increaseBase = 0;
    private double moreBase = 1;
    private double moreFinal = 1;
    private double addFinal = 0;

    // ===== DEFENSE NUMERIC BUCKETS =====
    private double baseDef = 0;
    private double addBaseDef = 0;
    private double increaseBaseDef = 0;
    private double moreBaseDef = 1;
    private double moreFinalDef = 1;
    private double addFinalDef = 0;

    private double finalDamage = 0;
    private double finalDef = 0;

    public DamageEvent(Entity victim, @Nullable Entity damager, double initialDamage, DamageType damageType) {
        this.victim = victim;
        this.damager = damager;
        this.initialDamage = initialDamage;
        this.damageType = damageType;
    }

    public void addDamageModifier(double value, DamageOperation operation) {
        needUpdate = true;
        switch (operation) {
            case BASE -> baseOverride = value;
            case ADD_BASE -> addBase += value;
            case INCREASE_BASE -> increaseBase += value;
            case MORE_BASE -> moreBase *= value;
            case MORE_FINAL -> moreFinal *= value;
            case ADD_FINAL -> addFinal += value;
        }
    }

    public void addDefenseModifier(double value, DamageOperation operation) {
        needUpdate = true;
        switch (operation) {
            case BASE -> baseDef = value;
            case ADD_BASE -> addBaseDef += value;
            case INCREASE_BASE -> increaseBaseDef += value;
            case MORE_BASE -> moreBaseDef *= value;
            case MORE_FINAL -> moreFinalDef *= value;
            case ADD_FINAL -> addFinalDef += value;
        }
    }

    private double calculateFinalDamage() {
        if (!needUpdate) return finalDamage;
        needUpdate = false;
        double base = (baseOverride >= 0) ? baseOverride : initialDamage;

        finalDamage = base + addBase;
        finalDamage *= (1 + increaseBase);
        finalDamage *= moreBase;
        finalDamage *= moreFinal;

        // Vanilla mechanics
        if (damager instanceof Player bukkitPlayer) {
            finalDamage *= getMeleeCooldownMultiplier(bukkitPlayer);
            if (isCritical) finalDamage *= 1.5;
        } else if (damager instanceof AbstractArrow arrow && !(arrow instanceof Trident)) {
            finalDamage *= getArrowVelocityMultiplier(arrow);
        }

        finalDamage += addFinal;

        if (victim instanceof Player) {
            double totalDef = calculateFinalDefense();
            finalDamage = applyDefense(finalDamage, totalDef);
        }

        return finalDamage;
    }

    private double calculateFinalDefense() {
        if (!needUpdate) return finalDef;
        needUpdate = false;
        finalDef = baseDef + addBaseDef;
        finalDef *= (1 + increaseBaseDef);
        finalDef *= moreBaseDef;
        finalDef *= moreFinalDef;
        finalDef += addFinalDef;
        return finalDef;
    }

    private static double getMeleeCooldownMultiplier(Player player) {
        float p = player.getAttackCooldown();
        return 0.2 + 0.8 * p * p;
    }

    private static double getArrowVelocityMultiplier(AbstractArrow arrow) {
        double speedSq = arrow.getVelocity().lengthSquared();
        double fullChargeThreshold = 8; // ~2.84
        return Math.clamp(speedSq / fullChargeThreshold, 0.2, 1.0);
    }

    private static double applyDefense(double damage, double defense) {
        double C = 15d;
        return damage * (C / (C + defense));
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public Entity getVictim() {
        return victim;
    }

    public @Nullable Entity getDamager() {
        return damager;
    }

    public double getInitialDamage() {
        return initialDamage;
    }

    public double getFinalDamage() {
        return calculateFinalDamage();
    }

    public double getFinalDef() {
        return calculateFinalDefense();
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
