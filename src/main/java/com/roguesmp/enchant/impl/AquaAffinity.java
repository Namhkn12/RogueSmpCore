package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AquaAffinity implements SmpEnchant {

    @Override
    public @NotNull String getId() {
        return "aqua_affinity";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.AQUA_AFFINITY;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Đào nhanh dưới nước";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng thời gian thở dưới nước thêm 15s mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.SEA_LANTERN;
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.HEAD);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.SUBMERGED_MINING_SPEED, 4d);
    }
}