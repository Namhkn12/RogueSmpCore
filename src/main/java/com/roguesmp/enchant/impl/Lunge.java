package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lunge implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "lunge";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.LUNGE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Lao tới";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động giống vanilla";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_SPEAR;
    }

    @Override
    public void attachVanillaEnchant(Map<Enchantment, Integer> currentVanillaEnchants, int level) {
        currentVanillaEnchants.merge(Enchantment.LUNGE, level, (integer, integer2) -> {
            if (integer + integer2 == 0) return null;
            return integer + integer2;
        });
    }
}
