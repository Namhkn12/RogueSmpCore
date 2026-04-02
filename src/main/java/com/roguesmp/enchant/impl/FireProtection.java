package com.roguesmp.enchant.impl;

import com.roguesmp.constant.*;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
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

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.FIRE) {
            // Vanilla: 8% damage reduction per level (EPF)
            // Use this scaling to match Vanilla's ~8% per level curve
            // Level 1: ~1.3, Level 4: ~7.0
            double bonusDefense = (level * 1.2) + (level * level * 0.13);
            event.addDefenseModifier(bonusDefense, DamageOperation.ADD_BASE);
        }
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.BURNING_TIME, - level * 0.1);
    }
}
