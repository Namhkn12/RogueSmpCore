package com.roguesmp.enchant.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.enchant.Enchants;
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

public class SweepingEdge implements SmpEnchant {

    @Override public @NotNull String getId() { return "sweeping_edge"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.SWEEPING_EDGE; }
    @Override public @NotNull String getSimpleName() { return "Quét cạnh"; }

    @Override
    public @NotNull List<Component> getDescription(int level) {
        return List.of(
                Component.text("Tăng chỉ số " + Attributes.SWEEPING_DAMAGE_RATIO.getAttribute().getSimpleName()),
                Component.text("thêm 0.1 mỗi cấp")
        );
    }

    @Override
    public Material getIcon() {
        return Material.WIND_CHARGE;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.SWEEPING_DAMAGE_RATIO, level * 0.1);
    }
}
