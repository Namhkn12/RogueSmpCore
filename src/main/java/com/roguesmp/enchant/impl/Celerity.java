package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Celerity implements SmpEnchant {

    private static final double VALUE_PER_LEVEL = 0.01;

    @Override
    public @NotNull String getId() {
        return "celerity";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.CELERITY;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Thần tốc";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng tốc độ di chuyển thêm " + Utils.formatDecimal(VALUE_PER_LEVEL * 100) + "% mỗi cấp";
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
        return Map.of(Attributes.SPEED_PERCENT, VALUE_PER_LEVEL * level);
    }


}
