package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class FireSlayer implements SmpEnchant {

    @Override
    public @NotNull String getId() {
        return "fire_slayer";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.FIRE_SLAYER;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Diệt hỏa";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND, EquipSlot.PROJECTILE);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng 2 sát thương gốc gây ra cho quái hệ lửa mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.BLAZE_POWDER;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() != DamageType.MELEE && event.getDamageType() != DamageType.PROJECTILE) return;
        if (!EntityUtils.isFire(event.getVictim())) return;
        event.addDamageModifier(2 * level, DamageOperation.ADD_BASE);
    }
}
