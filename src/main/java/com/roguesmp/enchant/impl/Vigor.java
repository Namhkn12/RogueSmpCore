package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Vigor implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "vigor";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.VIGOR;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Cường lực";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nhận 1% sát thương cận chiến mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.STONE_AXE;
    }


    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        Map<Attributes, Double> map = new EnumMap<>(Attributes.class);
        map.put(Attributes.MELEE_DAMAGE_PERCENT, 1d * level);
        return map;
    }
}
