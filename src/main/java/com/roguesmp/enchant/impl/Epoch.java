package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Epoch implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "epoch";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.EPOCH;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc thời";
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
        return "Giảm thời gian hồi chiêu 1% mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.CLOCK;
    }

    @Override
    public void onAbilityCast(AbilityCastEvent event, int level, @NotNull SmpPlayer player) {
        Ability ability = event.getAbility();
        ability.setCooldownTick((int) (ability.getCooldownTick() * (1 - 0.01 * level)));
    }
}
