package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Mending implements SmpEnchant {

    @Override public @NotNull String getId() { return "mending"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.MENDING; }
    @Override public @NotNull String getSimpleName() { return "Tu sửa"; }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override public void attachVanillaEnchant(Map<Enchantment, Integer> vanillaMap, int level) {
        vanillaMap.put(Enchantment.MENDING, 1);
    }

    @Override public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }
}
