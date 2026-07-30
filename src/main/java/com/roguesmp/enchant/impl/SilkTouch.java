package com.roguesmp.enchant.impl;

import com.roguesmp.enchant.Enchants;
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

public class SilkTouch implements SmpEnchant {

    @Override public @NotNull String getId() { return "silk_touch"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.SILK_TOUCH; }
    @Override public @NotNull String getSimpleName() { return "Độ mềm mại"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động giống vanilla";
    }

    @Override
    public Material getIcon() {
        return Material.WHITE_WOOL;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> vanillaMap, int level) {
        vanillaMap.put(Enchantment.SILK_TOUCH, level);
    }
}
