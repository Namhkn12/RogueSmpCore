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

public class Fortitude implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "fortitude";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.FORTITUDE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Kiên cố";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nhận 0.5 phòng ngự mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_CHESTPLATE;
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
        return Map.of(Attributes.DEFENSE_FLAT, 0.5 * level);
    }
}
