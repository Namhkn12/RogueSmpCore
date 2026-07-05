package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.Enchants;
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

public class BlastProtection implements SmpEnchant {

    public static double DEF_PER_LVL = 6;

    @Override
    public @NotNull String getId() {
        return "blast_protection";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.BLAST_PROTECTION;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Bảo vệ khỏi vụ nổ";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nhận +" + Utils.formatDecimal(DEF_PER_LVL) + "phòng thủ khi nhận sát thương nổ";
    }

    @Override
    public Material getIcon() {
        return Material.TNT;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.HEAD, EquipSlot.CHEST, EquipSlot.LEGS, EquipSlot.FEET, EquipSlot.OFFHAND, EquipSlot.MAINHAND);
    }

    @Override
    public void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.BLAST) {
            double bonusDefense = level * DEF_PER_LVL;
            event.addDefenseModifier(bonusDefense, DamageOperation.ADD_BASE);
        }
    }
}
