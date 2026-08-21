package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Uncritable implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "uncritable";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.UNCRITABLE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Không thể chí mạng";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Vũ khí sẽ không thể gây sát thương chí mạng";
    }

    @Override
    public Material getIcon() {
        return Material.BARRIER;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        event.setCritical(false);
    }
}
