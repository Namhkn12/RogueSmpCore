package com.roguesmp.event;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageOrigin;
import com.roguesmp.constant.DamageType;
import org.bukkit.damage.DamageSource;
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
        private final DamageOrigin origin;
        private boolean scalable = true;
        private boolean isBlockable;
        private boolean ignoreIframe;
        private boolean doKnockback;

        private Metadata(DamageType damageType, DamageOrigin origin, @Nullable String mobSpellId, @Nullable String abilityId) {
            this.damageType = damageType;
            this.origin = origin;
            this.mobSpellId = mobSpellId;
            this.abilityId = abilityId;
            this.isBlockable = damageType.isBlockableByDefault();
            this.doKnockback = damageType.knocksBackByDefault();
        }

        public static Metadata attack(DamageType damageType) {
            return new Metadata(damageType, DamageOrigin.ATTACK, null, null);
        }

        public static Metadata environment(DamageType damageType) {
            return new Metadata(damageType, DamageOrigin.ENVIRONMENT, null, null);
        }

        public static Metadata staff() {
            return new Metadata(DamageType.MAGIC, DamageOrigin.STAFF, null, null);
        }

        public static Metadata ability(DamageType damageType, @Nullable String abilityId) {
            return new Metadata(damageType, DamageOrigin.ABILITY, null, abilityId);
        }

        public static Metadata spell(DamageType damageType, @Nullable String mobSpellId) {
            return new Metadata(damageType, DamageOrigin.SPELL, mobSpellId, null);
        }

        public static Metadata enchant(DamageType damageType) {
            return new Metadata(damageType, DamageOrigin.ENCHANT, null, null);
        }

        public static Metadata effect(DamageType damageType) {
            return new Metadata(damageType, DamageOrigin.EFFECT, null, null);
        }

        public DamageOrigin getOrigin() {
            return origin;
        }

        public boolean isScalable() {
            return scalable;
        }

        public Metadata setScalable(boolean scalable) {
            this.scalable = scalable;
            return this;
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

        public boolean isDoKnockback() {
            return doKnockback;
        }

        public void setDoKnockback(boolean doKnockback) {
            this.doKnockback = doKnockback;
        }
    }

    private final Entity victim;
    private final @Nullable Entity damager;
    private final double initialDamage;
    private final Metadata metadata;
    private @Nullable DamageSource damageSource;

    private boolean needUpdateDmg = true;
    private boolean needUpdateDef = true;

    private boolean isBlocked;
    private double shieldBlockRatio;
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
        needUpdateDmg = true; // Defense changes require recalculating damage
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
        if (isBasic(DamageType.MELEE) && damager instanceof Player) {
            if (isCritical) finalDamage *= 1.5;
        }

        finalDamage += addFinal;

        if (victim instanceof Player) {
            double totalDef = calculateFinalDefense();
            finalDamage = applyDefense(finalDamage, totalDef);
        }

        finalDamage = Math.max(0.001, finalDamage); // Prevent negative damage in state
        needUpdateDmg = false;
        return finalDamage;
    }

    private double calculateFinalDefense() {
        if (!needUpdateDef) return finalDef;
        finalDef = baseDef + addBaseDef;
        finalDef *= (1 + increaseBaseDef);
        finalDef *= moreBaseDef;
        finalDef *= moreFinalDef;
        finalDef += addFinalDef;
        needUpdateDef = false;
        return finalDef;
    }

    private static double applyDefense(double damage, double defense) {
        final double C = 20d;
        return damage * (C / (C + defense));
    }

    public void setCritical(boolean critical) {
        if (this.isCritical != critical) {
            this.isCritical = critical;
            this.needUpdateDmg = true; //Invalidate damage cache on critical state change
        }
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

    public DamageOrigin getOrigin() {
        return metadata.origin;
    }

    public boolean isScalable() {
        return metadata.scalable;
    }

    public boolean isBasic(DamageType type) {
        return metadata.damageType == type && metadata.origin.isVanilla();
    }

    public boolean isScalable(DamageType type) {
        return metadata.damageType == type && metadata.scalable;
    }

    public boolean isShieldBlocked() {
        return shieldBlockRatio > 0;
    }

    public boolean isFullyShieldBlocked() {
        return shieldBlockRatio >= 1;
    }

    public double getShieldBlockRatio() {
        return shieldBlockRatio;
    }

    public void setShieldBlockRatio(double shieldBlockRatio) {
        this.shieldBlockRatio = shieldBlockRatio;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        if (blocked) {
            this.addDamageModifier(0, DamageOperation.MORE_FINAL);
        }
    }

    public @Nullable DamageSource getDamageSource() {
        return damageSource;
    }

    public void setDamageSource(@Nullable DamageSource damageSource) {
        this.damageSource = damageSource;
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
