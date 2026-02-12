package com.roguesmp.context;

import com.roguesmp.damage.DamageModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom damage event for more control
 */
public class DamageContext {
    private final Entity victim;
    private final Entity damager;
    private final double baseDamage;

    private boolean ignoreIframe;
    private boolean isCancelled;
    private boolean isCritical;

    private final List<DamageModifier> damageModifiers = new ArrayList<>();

    public DamageContext(Entity victim, Entity damager, double baseDamage) {
        this.victim = victim;
        this.damager = damager;
        this.baseDamage = baseDamage;
    }

    public void setIgnoreIframe(boolean ignoreIframe) {
        this.ignoreIframe = ignoreIframe;
    }

    public void addDamageModifier(DamageModifier damageModifier) {
        damageModifiers.add(damageModifier);
    }

    public double calculateFinalDamage() {
        double initialDmg = baseDamage;
        double additiveBonus = 0d;
        for (DamageModifier damageModifier : damageModifiers) {
            switch (damageModifier.operation()) {
                case ADDITIVE -> additiveBonus += damageModifier.value();
                case MULTIPLICATIVE -> initialDmg *= 1 + damageModifier.value();
            }
        }
        double finalDamage;
        if (damager instanceof Player player) {
            // If melee
            finalDamage = getDamageWithCooldown(initialDmg + additiveBonus, player);
            if (isCritical()) finalDamage = finalDamage * 1.5;
        } else finalDamage = initialDmg + additiveBonus;

        return finalDamage;
    }

    private static double getDamageWithCooldown(double baseDamage, Player player) {
        float p = player.getAttackCooldown();
        player.sendMessage(String.valueOf(p));
        return baseDamage * (0.2 + 0.8 * p * p);
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
