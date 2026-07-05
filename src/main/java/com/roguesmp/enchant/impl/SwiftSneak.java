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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwiftSneak implements SmpEnchant {

    @Override public @NotNull String getId() { return "swift_sneak"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.SWIFT_SNEAK; }
    @Override public @NotNull String getSimpleName() { return "Bò nhanh"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng chỉ số " + Attributes.SNEAKING_SPEED.getAttribute().getSimpleName() + " thêm 0.15 mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.LEGS);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.SNEAKING_SPEED, level * 0.15);
    }
}
