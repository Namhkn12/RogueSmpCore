package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectileProtection implements SmpEnchant {

    @Override public @NotNull String getId() { return "projectile_protection"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.PROJECTILE_PROTECTION; }
    @Override public @NotNull String getSimpleName() { return "Bảo vệ đạn đạo"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nhận 5 phòng ngự khi nhận sát thương tầm xa";
    }

    @Override
    public Material getIcon() {
        return Material.SHIELD;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.HEAD, EquipSlot.CHEST, EquipSlot.LEGS, EquipSlot.FEET);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> vanillaMap, int level) {
        vanillaMap.put(Enchantment.PROJECTILE_PROTECTION, level);
    }

    @Override
    public void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.PROJECTILE) {

            // Vanilla-matching scaling: +3.0 Defense per level
            // Level IV (4) = +12.0 Defense
            // Formula: 15 / (15 + 12) = 55% damage taken (45% reduction)
            double bonusDefense = level * 5.0;
            event.addDefenseModifier(bonusDefense, DamageOperation.ADD_BASE);
        }
    }
}
