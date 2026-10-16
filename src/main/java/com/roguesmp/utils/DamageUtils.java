package com.roguesmp.utils;

import com.roguesmp.constant.DamageFlag;
import com.roguesmp.event.DamageEvent;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageUtils {

    private static @Nullable DamageEvent.Metadata pendingMetadata;

    public static @Nullable DamageEvent.Metadata consumeMetadata() {
        DamageEvent.Metadata metadata = pendingMetadata;
        pendingMetadata = null;
        return metadata;
    }

    public static void damage(@NotNull LivingEntity victim, @Nullable Entity damager, double damage, DamageEvent.Metadata metadata) {
        if (!victim.isValid()) return;
        DamageEvent.Metadata previous = pendingMetadata;
        pendingMetadata = metadata;
        victim.damage(damage, createSource(damager, metadata));
        pendingMetadata = previous;
    }

    private static DamageSource createSource(@Nullable Entity damager, DamageEvent.Metadata metadata) {
        int flags = 0;
        if (!metadata.isDoKnockback()) flags |= DamageFlag.NO_KNOCKBACK.mask();
        if (metadata.isIgnoreIframe()) flags |= DamageFlag.BYPASS_COOLDOWN.mask();
        if (!metadata.isBlockable()) flags |= DamageFlag.UNBLOCKABLE.mask();

        DamageSource.Builder builder = DamageSource.builder(metadata.getDamageType().asVanilla(flags));
        if (damager != null) {
            Entity causing = damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter ? shooter : damager;
            builder.withDirectEntity(damager).withCausingEntity(causing);
        }
        return builder.build();
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
