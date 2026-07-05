package com.roguesmp.event;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
        private @Nullable String mobSpellId;
        private @Nullable String abilityId;
        private final DamageType damageType;
        private boolean isBlockable = true;
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

        public void setIgnoreIframe(boolean ignoreIframe) {
            this.ignoreIframe = ignoreIframe;
        }

        public @Nullable String getMobSpellId() {
            return mobSpellId;
        }

        public @Nullable String getAbilityId() {
            return abilityId;
        }

        public DamageType getDamageType() {
            return damageType;
        }

        public boolean isBlockable() {
            return isBlockable;
        }

        public void setBlockable(boolean blockable) {
            isBlockable = blockable;
        }
    }

    private final Entity victim;
    private final @Nullable Entity damager;
    private final double initialDamage;
    private final Metadata metadata;

    private boolean needUpdateDmg = true; // For recalculating dmg value
    private boolean needUpdateDef = true;

    private boolean isBlocked;
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
        needUpdateDmg = true;
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
        needUpdateDef = true;
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
        if (!needUpdateDmg) return finalDamage;
        double base = (baseOverride >= 0) ? baseOverride : initialDamage;

        finalDamage = base + addBase;
        finalDamage *= (1 + increaseBase);
        finalDamage *= moreBase;
        finalDamage *= moreFinal;

        // Vanilla mechanics
        if (metadata.damageType == DamageType.MELEE && damager instanceof Player bukkitPlayer) {
            if (isCritical) finalDamage *= 1.5;
        }

        finalDamage += addFinal;

        if (victim instanceof Player) {
            double totalDef = calculateFinalDefense();
            finalDamage = applyDefense(finalDamage, totalDef);
        }

        needUpdateDmg = false;
        return Math.max(0.001, finalDamage); // Prevent negative damage
    }

    private double calculateFinalDefense() {
        if (!needUpdateDef) return finalDef;
        finalDef = baseDef + addBaseDef;
        finalDef *= (1 + increaseBaseDef);
        finalDef *= moreBaseDef;
        finalDef *= moreFinalDef;
        finalDef += addFinalDef;
        return finalDef;
    }

    private static double applyDefense(double damage, double defense) {
        final double C = 20d;
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

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        if (blocked) {
            // Leave it as complete negation for now, might add a percent reduction in the future
            this.addDamageModifier(0, DamageOperation.MORE_FINAL);
        }
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
