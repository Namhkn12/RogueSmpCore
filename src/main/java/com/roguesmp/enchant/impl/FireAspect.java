package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
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

public class FireAspect implements SmpEnchant {

    @Override public @NotNull String getId() { return "fire_aspect"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.FIRE_ASPECT; }
    @Override public @NotNull String getSimpleName() { return "Khía cạnh của lửa"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Là phù phép vanilla <:";
    }

    @Override
    public Material getIcon() {
        return Material.MAGMA_CREAM;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> vanillaMap, int level) {
        vanillaMap.put(Enchantment.FIRE_ASPECT, level);
    }
}