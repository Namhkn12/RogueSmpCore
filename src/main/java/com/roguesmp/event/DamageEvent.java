package com.roguesmp.event;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import org.bukkit.attribute.Attribute;
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

    public static class Metadata {
        private final @Nullable String mobSpellId;
        private final @Nullable String abilityId;
        private DamageType damageType;
        private boolean ignoreIframe;

        public Metadata(DamageType damageType) {
            this(damageType, null, null);
        }

        public Metadata(@Nullable String mobSpellId, @Nullable String abilityId, DamageType damageType, boolean ignoreIframe) {
            this.mobSpellId = mobSpellId;
            this.abilityId = abilityId;
            this.damageType = damageType;
            this.ignoreIframe = ignoreIframe;
        }

        public Metadata(@Nullable String abilityId, DamageType damageType) {
            this(damageType, null, abilityId);
        }

        public Metadata(DamageType damageType, @Nullable String mobSpellId) {
            this(damageType, mobSpellId, null);
        }

        public Metadata(DamageType damageType, @Nullable String mobSpellId, @Nullable String abilityId) {
            this(mobSpellId, abilityId, damageType, false);
        }

        public boolean isIgnoreIframe() {
            return ignoreIframe;
        }
    }

    private final Entity victim;
    private final @Nullable Entity damager;
    private final double initialDamage;
    private final Metadata metadata;

    private boolean needUpdate = true; // For recalculating dmg value

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

    public DamageEvent(Entity victim, @Nullable Entity damager, double initialDamage, @NotNull Metadata metadata) {
        this.victim = victim;
        this.damager = damager;
        this.initialDamage = initialDamage;
        this.metadata = metadata;
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
        double base = (baseOverride >= 0) ? baseOverride : initialDamage;

        finalDamage = base + addBase;
        finalDamage *= (1 + increaseBase);
        finalDamage *= moreBase;
        finalDamage *= moreFinal;

        // Vanilla mechanics
        if (metadata.damageType == DamageType.MELEE && damager instanceof Player bukkitPlayer) {
            finalDamage *= getMeleeCooldownMultiplier(bukkitPlayer);
            if (isCritical && bukkitPlayer.getAttribute(Attribute.ATTACK_DAMAGE).getValue() <= 1d) finalDamage *= 1.5;
        } else if (damager instanceof AbstractArrow arrow && !(arrow instanceof Trident)) {
            finalDamage *= getArrowVelocityMultiplier(arrow);
        }

        finalDamage += addFinal;

        if (victim instanceof Player) {
            double totalDef = calculateFinalDefense();
            finalDamage = applyDefense(finalDamage, totalDef);
        }

        needUpdate = false;
        return finalDamage;
    }

    private double calculateFinalDefense() {
        if (!needUpdate) return finalDef;
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

    public boolean isIgnoreIframe() {
        return metadata.ignoreIframe;
    }

    public DamageEvent setIgnoreIframe(boolean ignoreIframe) {
        metadata.ignoreIframe = ignoreIframe;
        return this;
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
        return metadata.damageType;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
