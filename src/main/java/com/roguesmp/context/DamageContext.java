package com.roguesmp.context;

import com.destroystokyo.paper.event.player.PlayerAttackEntityCooldownResetEvent;
import com.roguesmp.damage.DamageModifier;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom damage event for more control
 */
public class DamageContext {
    private Entity victim;
    private Entity damager;
    private Entity directDamager;
    private boolean ignoreIframe;
    private boolean isCritical;
    private final double baseDamage;
    private double finalDamage;
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
        if (damager instanceof Player player && !(directDamager instanceof Projectile)) {
            // If melee
            finalDamage = getDamageWithCooldown(initialDmg + additiveBonus, player);
        } else finalDamage = initialDmg + additiveBonus;

        return finalDamage;
    }

    private double getDamageWithCooldown(double baseDamage, Player player) {
        float p = player.getAttackCooldown();// damage-accurate value
        player.sendMessage(String.valueOf(p));
        return baseDamage * (0.2 + 0.8 * p * p);
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }
}
