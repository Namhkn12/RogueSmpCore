package com.roguesmp.context;

import com.roguesmp.damage.DamageModifier;
import com.roguesmp.damage.DefenseModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom damage event for more control
 */
public class DamageContext {
    private final Entity victim;
    private final Entity damager;
    private final double initialDamage;

    private boolean ignoreIframe;
    private boolean isCancelled;
    private boolean isCritical;

    private final List<DamageModifier> damageModifiers = new ArrayList<>();
    private final List<DefenseModifier> defenseModifiers = new ArrayList<>();

    public DamageContext(Entity victim, Entity damager, double initialDamage) {
        this.victim = victim;
        this.damager = damager;
        this.initialDamage = initialDamage;
    }

    public void setIgnoreIframe(boolean ignoreIframe) {
        this.ignoreIframe = ignoreIframe;
    }

    public void addDamageModifier(DamageModifier damageModifier) {
        damageModifiers.add(damageModifier);
    }

    public void addDefenseModifier(DefenseModifier defenseModifier) {
        defenseModifiers.add(defenseModifier);
    }

    public double calculateFinalDamage() {
        double base = initialDamage;

        double addBase = 0d;
        double increaseBase = 0d;
        double moreBase = 1d;

        double moreFinal = 1d;
        double addFinal = 0d;

        for (DamageModifier modifier : damageModifiers) {
            double value = modifier.value();
            switch (modifier.operation()) {
                case BASE -> base = value;
                case ADD_BASE -> addBase += value;
                case INCREASE_BASE -> increaseBase += value;
                case MORE_BASE -> moreBase *= value;
                case MORE_FINAL -> moreFinal *= value;
                case ADD_FINAL -> addFinal += value;
            }
        }

        double total = base + addBase;
        total *= (1 + increaseBase);
        total *= moreBase;
        total *= moreFinal;

        // Post-processing
        if (damager instanceof Player player) {
            total = total * getMeleeCooldownMultiplier(player);
            if (isCritical()) total *= 1.5;
        } else if (damager instanceof AbstractArrow arrow && !(arrow instanceof Trident)) {
             total = total * getArrowVelocityMultiplier(arrow);
        }

        total += addFinal;

        if (victim instanceof Player player) { // Defense calculation
            double totalDef = getTotalDef();
            total = applyDefense(total, totalDef);
            player.sendMessage(String.valueOf(total));
        }

        return total;
    }

    private double getTotalDef() {
        double baseDef = 0;

        double addBaseDef = 0d;
        double increaseBaseDef = 0d;
        double moreBaseDef = 1d;

        double moreFinalDef = 1d;
        double addFinalDef = 0d;

        for (DefenseModifier modifier : defenseModifiers) {
            double value = modifier.value();
            switch (modifier.operation()) {
                case BASE -> baseDef = value;
                case ADD_BASE -> addBaseDef += value;
                case INCREASE_BASE -> increaseBaseDef += value;
                case MORE_BASE -> moreBaseDef *= value;
                case MORE_FINAL -> moreFinalDef *= value;
                case ADD_FINAL -> addFinalDef += value;
            }
        }

        double totalDef = baseDef + addBaseDef;
        totalDef *= (1 + increaseBaseDef);
        totalDef *= moreBaseDef;
        totalDef *= moreFinalDef;
        totalDef += addFinalDef;
        return totalDef;
    }

    private static double getMeleeCooldownMultiplier(Player player) {
        float p = player.getAttackCooldown();
        return 0.2 + 0.8 * p * p;
    }

    /**
     * Trident also extend AbstractArrow, make sure to check for that before using this
     */
    private static double getArrowVelocityMultiplier(AbstractArrow arrow) {
        double speedSq = arrow.getVelocity().lengthSquared();
        double fullChargeThreshold = 8; // ~2.84 velocity

        return Math.clamp(speedSq / fullChargeThreshold, 0.2, 1.0);

    }

    private static double applyDefense(double damage, double defense) {
        double C = 15d;
        return damage * (C / (C + defense)); // defense formula
    }

    public boolean isCritical() {
        return isCritical;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
