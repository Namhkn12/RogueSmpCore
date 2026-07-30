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

public class Focus implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "focus";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.FOCUS;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Định tâm";
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
    public @NotNull String getSimpleDescription() {
        return "Nhận 1% sát thương tầm xa mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.TARGET;
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Map.of(Attributes.PROJECTILE_DAMAGE_PERCENT, 0.01 * level);
    }
}
