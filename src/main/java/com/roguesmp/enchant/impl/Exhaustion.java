package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Exhaustion implements SmpEnchant {

    private static final EnumSet<DamageType> affectedTypes = EnumSet.of(
            DamageType.MELEE, DamageType.MELEE_ABILITY, DamageType.PROJECTILE, DamageType.PROJECTILE_ABILITY, DamageType.MAGIC
    );

    @Override
    public @NotNull String getId() {
        return "exhaustion";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.EXHAUSTION;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Lời nguyền kiệt sức";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultNegativeLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Khi tấn công sẽ tiêu hao 0.5 điểm đói mỗi cấp, nếu không còn điểm đói sẽ không thể gây sát thương nữa";
    }

    @Override
    public Material getIcon() {
        return Material.ROTTEN_FLESH;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (!affectedTypes.contains(event.getDamageType())) return;
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer.getGameMode() == GameMode.CREATIVE) return;

        float totalToDeduct = 0.5f * level;

        float currentSaturation = bukkitPlayer.getSaturation();
        int currentFood = bukkitPlayer.getFoodLevel();

        // Player can't deal damage if they has no food remaining
        if (currentFood <= 0 && currentSaturation <= 0.0f) {
            event.setCancelled(true);
            return;
        }

        // Deduct from Saturation first
        if (currentSaturation > 0.0f) {
            if (currentSaturation >= totalToDeduct) {
                bukkitPlayer.setSaturation(currentSaturation - totalToDeduct);
                totalToDeduct = 0.0f;
            } else {
                totalToDeduct -= currentSaturation;
                bukkitPlayer.setSaturation(0.0f);
            }
        }

        // Deduct remaining balance from Food Level
        if (totalToDeduct > 0.0f) {
            int foodToDeduct = (int) Math.ceil(totalToDeduct);
            int newFoodLevel = Math.max(0, currentFood - foodToDeduct);
            bukkitPlayer.setFoodLevel(newFoodLevel);
        }
    }
}
