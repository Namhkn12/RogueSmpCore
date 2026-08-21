package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.PotentPoisonEffect;
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

public class Poisoning implements SmpEnchant {

    private static final String SOURCE = "enchant_poisoning";

    @Override
    public @NotNull String getId() {
        return "poisoning";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.POISONING;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Kịch độc";
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
        return "Tấn công full lực trúng kẻ địch gây cộng dồn hiệu ứng Kịch độc kéo dài 2s. Tối đa 5 stack, mỗi stack gây (cấp) sát thương";
    }

    @Override
    public Material getIcon() {
        return SmpEnchant.super.getIcon();
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        DamageType damageType = event.getDamageType();
        if (damageType != DamageType.MELEE && damageType != DamageType.PROJECTILE) return;
        if (!(event.getVictim() instanceof LivingEntity victim)) return;
        if (player.getBukkitPlayer().getAttackCooldown() < 0.96) return;

        SmpEffect existing = EffectManager.getInstance().getActiveEffects(victim).get(SOURCE);
        if (existing instanceof PotentPoisonEffect poison) {
            poison.addStack();
        } else {
            PotentPoisonEffect potentPoisonEffect = new PotentPoisonEffect(PotentPoisonEffect.DURATION_TICKS, level, 1);
            potentPoisonEffect.setApplier(player.getBukkitPlayer());
            EffectManager.getInstance().addEffect(victim, SOURCE, potentPoisonEffect);
        }
    }
}
