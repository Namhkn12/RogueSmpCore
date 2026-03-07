package com.roguesmp.utils;

import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageUtils {

    public static DamageEvent.Metadata nextMetadata = null; // Used to fill in our damage listener with data from DamageUtils

    public static void damage(@NotNull LivingEntity victim, @Nullable Entity damager, double damage, DamageEvent.Metadata metadata) {
        if (!victim.isValid()) return;
        nextMetadata = metadata;
        victim.damage(damage, damager);
    }
}
