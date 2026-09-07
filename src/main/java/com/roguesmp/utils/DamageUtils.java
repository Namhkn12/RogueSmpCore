package com.roguesmp.utils;

import com.roguesmp.event.DamageEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageUtils {

    public static DamageEvent.Metadata nextMetadata = null; // Used to fill in our damage listener with data from DamageUtils

    public static void damage(@NotNull LivingEntity victim, @Nullable Entity damager, double damage, DamageEvent.Metadata metadata) {
        if (!victim.isValid()) return;
        nextMetadata = metadata;
        boolean bypassIFrame = metadata.isIgnoreIframe();

        int originalIFrame = victim.getNoDamageTicks();
        double originalLastDamage = victim.getLastDamage();

        if (bypassIFrame) {
            victim.setNoDamageTicks(0);
        }

        if (damager != null) {
            if (metadata.isDoKnockback()) {
                victim.damage(damage, damager);
            } else {
                Vector initialVel = victim.getVelocity();
                victim.damage(damage, damager);
                Vector zero = new Vector();
                victim.setVelocity(zero.add(initialVel));
            }
        } else {
            victim.damage(damage, (Entity) null);
        }

        nextMetadata = null;
        if (bypassIFrame) {
            victim.setNoDamageTicks(originalIFrame);
            victim.setLastDamage(originalLastDamage);
        }
    }

    /**
     * @param percentToDamage 0.1 = 10% damage
     */
    public static void damagePercent(@NotNull LivingEntity victim, @Nullable Entity damager, double percentToDamage, DamageEvent.Metadata metadata) {
        double damage = EntityUtils.getMaxHealth(victim) * percentToDamage;
        damage(victim, damager, damage, metadata);
    }

    public static void damage(@NotNull LivingEntity victim, @Nullable Entity damager, Location location, double damage, DamageEvent.Metadata metadata) {

    }
}
