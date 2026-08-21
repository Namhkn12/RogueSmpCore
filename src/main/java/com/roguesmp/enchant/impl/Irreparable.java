package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Curse - vetoes any durability repair (increase) attempt that goes through
 * {@link DurabilityChangedEvent}, by zeroing out a positive change amount. Doesn't affect normal
 * wear (negative change amounts pass through untouched). {@code ItemRepairGui} mutates durability
 * directly without firing the event, so it carries its own separate check for this curse.
 */
public class Irreparable implements SmpEnchant {

    @Override public @NotNull String getId() { return "irreparable"; }
    @Override public @NotNull Enchants getEnumConstant() { return Enchants.IRREPARABLE; }
    @Override public @NotNull String getSimpleName() { return "Không thể sửa"; }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Vật phẩm mang lời nguyền này không thể được sửa chữa bằng bất kỳ cách nào";
    }

    @Override
    public Material getIcon() {
        return Material.NETHERITE_SCRAP;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    @Override public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public void onDurabilityChange(DurabilityChangedEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getChangeAmount() > 0) {
            event.setChangeAmount(0);
        }
    }
}
