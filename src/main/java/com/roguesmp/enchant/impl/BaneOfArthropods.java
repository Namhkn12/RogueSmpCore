package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BaneOfArthropods implements SmpEnchant {

    public static double DMG_PER_LEVEL = 2;

    @Override
    public @NotNull String getId() {
        return "bane_of_arthropods";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.BANE_OF_ARTHROPODS;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Diệt chân đốt";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Sát thương gốc gây ra cho quái chân đốt tăng thêm 2 mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.COBWEB;
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        Entity target = event.getVictim();

        if (EntityUtils.isArthropod(target)) {
            double extraDamage = level * DMG_PER_LEVEL;
            event.addDamageModifier(extraDamage, DamageOperation.ADD_BASE);
        }
    }
}
