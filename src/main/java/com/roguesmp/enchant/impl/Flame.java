package com.roguesmp.enchant.impl;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Flame implements SmpEnchant {

    @Override public @NotNull String getId() { return "flame"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.FLAME; }
    @Override public @NotNull String getSimpleName() { return "Lửa"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Mũi tên bọc lửa, khiến kẻ địch bị cháy 5 giây mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.MAGMA_CREAM;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, int level, @NotNull SmpPlayer player) {
        Entity target = event.getHitEntity();
        if (target instanceof LivingEntity living) {
            // Flame sets target on fire for 5 seconds (100 ticks)
            living.setFireTicks(Math.max(living.getFireTicks(), 100));
        }
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, int level, @NotNull SmpPlayer player) {
        event.getProjectile().setFireTicks(400);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, int level, @NotNull SmpPlayer player) {
        event.getProjectile().setFireTicks(400);
    }
}
