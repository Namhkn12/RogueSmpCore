package com.roguesmp.utils;

import com.roguesmp.event.DamageEvent;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
            victim.damage(damage, damager);

            victim.setNoDamageTicks(originalIFrame);
            victim.setLastDamage(originalLastDamage);
        } else victim.damage(damage, damager);
    }

    public static void damage(@NotNull LivingEntity victim, @Nullable Entity damager, Location location, double damage, DamageEvent.Metadata metadata) {

    }
}
