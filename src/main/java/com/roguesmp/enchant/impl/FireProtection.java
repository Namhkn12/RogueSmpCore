package com.roguesmp.enchant.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.*;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FireProtection implements SmpEnchant {

    @Override public @NotNull String getId() { return "fire_protection"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.FIRE_PROTECTION; }
    @Override public @NotNull String getSimpleName() { return "Bảo vệ khỏi lửa"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nhận 4 phòng ngự mỗi cấp khi chịu sát thương lửa và giảm thời gian cháy 10% mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.MAGMA_BLOCK;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.FIRE) {
            event.addDefenseModifier(4 * level, DamageOperation.ADD_BASE);
        }
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.BURNING_TIME, - level * 0.1);
    }
}
