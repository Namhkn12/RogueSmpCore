package com.roguesmp.enchant.impl;

import com.roguesmp.constant.*;
import com.roguesmp.context.DamageContext;
import com.roguesmp.damage.DamageModifier;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Greed implements SmpEnchant {

    private final NamespacedKey DATA_KEY = new NamespacedKey(Keys.GLOBAL_NAMESPACE, "greed");

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
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Integer stack = pdc.get(DATA_KEY, PersistentDataType.INTEGER);
        return List.of(Component.text(getSimpleName() + " " + Utils.toRoman(level) + " (" + stack + ")", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.of(EquipSlot.MAINHAND);
    }

    @Override
    public void attachDefaultData(PersistentDataContainer pdc) {
        Integer stack = pdc.get(DATA_KEY, PersistentDataType.INTEGER);
        if (stack == null) pdc.set(DATA_KEY, PersistentDataType.INTEGER, 0);
    }

    @Override
    public void onMeleeDamageEntity(DamageContext context, int level, SmpPlayer player) {
        context.addDamageModifier(new DamageModifier(getId(), level, DamageType.PHYSICAL, DamageOperation.ADD_BASE));
    }

    @Override
    public void onProjectileDamageEntity(DamageContext context, int level, SmpPlayer player) {
        context.addDamageModifier(new DamageModifier(getId(), level, DamageType.PHYSICAL, DamageOperation.ADD_BASE));
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, int level, SmpPlayer player) {
        Player player1 = (Player) event.getDamageSource().getCausingEntity();
        player1.getEquipment().getItemInMainHand().editPersistentDataContainer(pdc -> {
            Integer stack = pdc.get(DATA_KEY, PersistentDataType.INTEGER);
            if (stack == null) return;
            pdc.set(DATA_KEY, PersistentDataType.INTEGER, ++stack);
        });
        event.setDeathSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK);
    }
}
