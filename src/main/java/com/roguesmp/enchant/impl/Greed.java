package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.DamageContext;
import com.roguesmp.damage.DamageModifier;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.enchant.StackingEnchant;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Greed implements SmpEnchant, StackingEnchant {

    @Override
    public @NotNull String getId() {
        return "greed";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.GREED;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tham lam";
    }

    @Override
    public @NotNull List<Component> getDisplayText(int level, SmpPlayer player, PersistentDataContainerView pdc) {
        return Utils.fromStrings("<b><blue><!i>Tham lam " + level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void onAttackEntity(DamageContext context, int level) {
        context.addDamageModifier(new DamageModifier("greed", level, DamageType.PHYSICAL, DamageOperation.ADDITIVE));
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, int level) {
        event.setDeathSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK);
    }

    @Override
    public void addData(ItemStack newStack, @Nullable ItemStack oldStack, SmpPlayer player, @NotNull PersistentDataContainer modifierData) {

    }
}
