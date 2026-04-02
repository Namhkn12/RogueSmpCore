package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Impaling implements SmpEnchant {

    public static double DMG_PER_LVL = 2;

    @Override public @NotNull String getId() { return "impaling"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.IMPALING; }
    @Override public @NotNull String getSimpleName() { return "Đâm vào da thịt"; }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        Entity target = event.getVictim();

        if (EntityUtils.isAquatic(target)) {
            double bonusDamage = level * DMG_PER_LVL;
            event.addDamageModifier(bonusDamage, DamageOperation.ADD_BASE);
        }
    }


}
