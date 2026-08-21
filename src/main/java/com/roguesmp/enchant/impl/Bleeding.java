package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.BleedingEffect;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Bleeding implements SmpEnchant {

    private static final int BLEED_DURATION_TICKS = 60; // 3 seconds

    @Override
    public @NotNull String getId() {
        return "bleeding";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.BLEEDING;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Thổ huyết";
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
        return "Tấn công trúng kẻ địch gán hiệu ứng Chảy máu kéo dài 3 giây, mỗi giây gây x sát thương mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.REDSTONE;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() != DamageType.MELEE && event.getDamageType() != DamageType.PROJECTILE) return;
        if (event.getVictim() instanceof LivingEntity victim) {
            EffectManager.getInstance().addEffect(victim, "enchant_bleeding", new BleedingEffect(BLEED_DURATION_TICKS, level, player.getBukkitPlayer()));
        }
    }
}
