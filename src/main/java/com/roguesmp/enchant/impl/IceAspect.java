package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class IceAspect implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "ice_aspect";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.ICE_ASPECT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Khía cạnh của băng";
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
        return "Kẻ địch sẽ bị làm chậm 10% mỗi cấp khi đánh trúng, kéo dài 2 giây";
    }

    @Override
    public Material getIcon() {
        return Material.PACKED_ICE;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() != DamageType.MELEE && event.getDamageType() != DamageType.PROJECTILE) return;
        EffectManager.getInstance().addEffect(event.getVictim(), "ice_aspect_slowness", new SpeedEffect(40, -0.1 * level, "ice_aspect_slowness"));
    }
}
