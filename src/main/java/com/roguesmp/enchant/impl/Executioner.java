package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Executioner implements SmpEnchant {

    private static final double HEALTH_THRESHOLD = 0.35;
    private static final double BONUS_PER_LEVEL = 0.1;

    @Override
    public @NotNull String getId() {
        return "executioner";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.EXECUTIONER;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Đoạt Mệnh";
    }

    @Override
    public @NotNull List<Component> getDescription(int level) {
        String bonus = level > 0 ? Math.round(BONUS_PER_LEVEL * level * 100) + "%" : "10% mỗi cấp";
        return List.of(
                Utils.fromString("Gây thêm <red>" + bonus + "</red> sát thương"),
                Component.text("lên kẻ địch dưới 35% máu.")
        );
    }

    @Override
    public Material getIcon() {
        return Material.WITHER_SKELETON_SKULL;
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
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (!event.isBasic(DamageType.MELEE) && !event.isBasic(DamageType.PROJECTILE)) return;
        if (!(event.getVictim() instanceof LivingEntity victim)) return;

        AttributeInstance maxHealth = victim.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || victim.getHealth() > maxHealth.getValue() * HEALTH_THRESHOLD) return;

        event.addDamageModifier(BONUS_PER_LEVEL * level, DamageOperation.INCREASE_BASE);
    }
}
