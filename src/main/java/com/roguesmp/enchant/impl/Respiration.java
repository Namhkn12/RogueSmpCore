package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Respiration implements SmpEnchant {

    @Override public @NotNull String getId() { return "respiration"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.RESPIRATION; }
    @Override public @NotNull String getSimpleName() { return "Hô hấp"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.HEAD);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.OXYGEN_BONUS, (double) level);
    }
}
