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

public class QuickCharge implements SmpEnchant {
    @Override public @NotNull String getId() { return "quick_charge"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.QUICK_CHARGE; }
    @Override public @NotNull String getSimpleName() { return "Nạp nhanh"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động giống vanilla";
    }

    @Override
    public Material getIcon() {
        return Material.SUGAR;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> currentVanillaEnchants, int level) {
        currentVanillaEnchants.put(Enchantment.QUICK_CHARGE, level);
    }

    @Override public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }
}
