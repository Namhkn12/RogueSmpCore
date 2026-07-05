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

public class FeatherFalling implements SmpEnchant {

    public static double MULT_PER_LVL = - 0.1; // Minus

    @Override public @NotNull String getId() { return "feather_falling"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.FEATHER_FALLING; }
    @Override public @NotNull String getSimpleName() { return "Bảo vệ khỏi sát thương rơi"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Giảm sát thương rơi phải nhận 10% mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.FEET);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        Map<Attributes, Double> attributesMap = new EnumMap<>(Attributes.class);
        attributesMap.put(Attributes.FALL_DAMAGE, MULT_PER_LVL * level);
        return attributesMap;
    }
}
