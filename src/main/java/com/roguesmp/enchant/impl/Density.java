package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Density implements SmpEnchant {

    public static final double DMG_PER_LEVEL = 0.5;

    @Override public @NotNull String getId() { return "density"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.DENSITY; }
    @Override public @NotNull String getSimpleName() { return "Tỉ trọng"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng sát thương gốc gây ra thêm " + Utils.formatDecimal(DMG_PER_LEVEL) + " * {khoảng cách rơi} mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.MACE;
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        // Vanilla: Adds 0.5 damage per block fallen per level.
        float fallDistance = player.getBukkitPlayer().getFallDistance();

        if (fallDistance > 0) {
            double bonusDamage = fallDistance * (level * DMG_PER_LEVEL);
            event.addDamageModifier(bonusDamage, DamageOperation.ADD_BASE);
        }
    }
}
