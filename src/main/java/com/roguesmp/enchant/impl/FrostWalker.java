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

public class FrostWalker implements SmpEnchant {

    @Override public @NotNull String getId() { return "frost_walker"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.FROST_WALKER; }
    @Override public @NotNull String getSimpleName() { return "Chân lạnh"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động như vanilla";
    }

    @Override
    public Material getIcon() {
        return Material.BLUE_ICE;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.FEET);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> vanillaMap, int level) {
        vanillaMap.put(Enchantment.FROST_WALKER, level);
    }
}
