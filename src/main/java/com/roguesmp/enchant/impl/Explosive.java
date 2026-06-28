package com.roguesmp.enchant.impl;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Explosive implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "explosive";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.EXPLOSIVE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tên nổ";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND, EquipSlot.PROJECTILE);
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        player.getBukkitPlayer().sendMessage("Damage! " + level);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, int level, @NotNull SmpPlayer player) {
        Player player1 = player.getBukkitPlayer();
        player1.playSound(player1 , Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, int level, @NotNull SmpPlayer player) {
        Player player1 = player.getBukkitPlayer();
        player1.playSound(player1 , Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, int level, @NotNull SmpPlayer player) {
        player.getBukkitPlayer().sendMessage("EXPLODE! " + level);
    }
}
