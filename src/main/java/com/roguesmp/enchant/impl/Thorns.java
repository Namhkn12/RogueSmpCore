package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.DamageUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Thorns implements SmpEnchant {

    @Override public @NotNull String getId() { return "thorns"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.THORNS; }
    @Override public @NotNull String getSimpleName() { return "Gai"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Khi chịu sát thương, kẻ địch gây sát thương đó sẽ phải chịu 1 sát thương mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.CACTUS;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override public void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity living) {
            DamageUtils.damage(living, player.getBukkitPlayer(), level, new DamageEvent.Metadata(DamageType.THORNS));
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) {
            DamageUtils.damage(living, player.getBukkitPlayer(), level, new DamageEvent.Metadata(DamageType.THORNS));
        }
    }
}
