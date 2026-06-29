package com.roguesmp.enchant.impl;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.VectorUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Multishot implements SmpEnchant {

    @Override public @NotNull String getId() { return "multishot"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.MULTISHOT; }
    @Override public @NotNull String getSimpleName() { return "Bắn nhiều tia"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, int level, @NotNull SmpPlayer player) {
        if (event.isCancelled()) return;
        handleMultishot(level, player, event.getProjectile());
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, int level, @NotNull SmpPlayer player) {
        if (event.isCancelled()) return;
        handleMultishot(level, player, (Projectile) event.getProjectile());
    }

    private static void handleMultishot(int level, @NotNull SmpPlayer player, Projectile primary) {
        if (EntityManager.getInstance().hasMetadata(primary, "multishot_guard")) {
            PlayerProjectile playerProjectile = player.getProjectile(primary.getUniqueId());
            if (playerProjectile != null) {
                playerProjectile.setReduceDurability(false);
            }
            return;
        }

        Vector direction = primary.getVelocity();

        double baseSpread = 10;

        for (int i = -level; i <= level; i++) {
            if (i == 0) continue;

            double angle = i * baseSpread;

            Vector spreadVelocity = VectorUtils.rotateYAxis(direction.clone().normalize(), angle)
                    .multiply(direction.length());
            // Safe because primary is always projectile
            player.getBukkitPlayer().launchProjectile(primary.getClass(), spreadVelocity, entity -> {
                EntityManager.getInstance().addMetadata(primary, "multishot_guard", true);
                entity.setVelocity(spreadVelocity);
                entity.setFireTicks(primary.getFireTicks());

                if (primary instanceof AbstractArrow arrow && entity instanceof AbstractArrow sideArrow) {
                    sideArrow.setCritical(arrow.isCritical());
                    sideArrow.setPierceLevel(arrow.getPierceLevel());
                    sideArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                    sideArrow.setItemStack(arrow.getItemStack());
                } else if (primary instanceof ThrowableProjectile throwable && entity instanceof ThrowableProjectile sideThrowable) {
                    sideThrowable.setItem(throwable.getItem());
                }
            });
//            Projectile shotProjectile = (Projectile) spawnLoc.getWorld().spawn(spawnLoc, primary.getType().getEntityClass(), entity -> {
//                Projectile projectile = (Projectile) entity;
//                projectile.setVelocity(spreadVelocity);
//                projectile.setFireTicks(primary.getFireTicks());
//
//                if (primary instanceof AbstractArrow arrow && entity instanceof AbstractArrow sideArrow) {
//                    sideArrow.setCritical(arrow.isCritical());
//                    sideArrow.setPierceLevel(arrow.getPierceLevel());
//                    sideArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
//                    sideArrow.setItemStack(arrow.getItemStack());
//                } else if (primary instanceof ThrowableProjectile throwable && entity instanceof ThrowableProjectile sideThrowable) {
//                    sideThrowable.setItem(throwable.getItem());
//                }
//            });
//            shotProjectile.setShooter(player.getBukkitPlayer());
//            player.trackProjectile(shotProjectile, player.getActiveEnchants(), player.getActiveAttributes());
        }
    }
}
